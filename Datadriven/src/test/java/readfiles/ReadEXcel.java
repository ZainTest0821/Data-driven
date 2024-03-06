package readfiles;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.*;
import java.time.Duration;

public class ReadEXcel {

    WebDriver driver;

    public ReadEXcel() throws IOException {
    }

    @BeforeTest
    public void launch() {
        System.setProperty("webdriver.http.factory", "jdk-http-client");
        WebDriverManager.chromedriver();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        driver.get("https://portal-test.homeapphub.com/anonymous/signup/roofwrightlite");
    }

    @Test(priority = 1)
    void login() {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
                .withTimeout(Duration.ofSeconds(20))
                .pollingEvery(Duration.ofSeconds(2))
                .ignoring(NoSuchElementException.class);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        WebElement firstname = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='FullName']")));
        firstname.sendKeys("Peter Willson");

        // Enter Email in email field
        WebElement email = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='EmailAddress']")));
        email.sendKeys("peterwillson123@gmail.com");

        // Enter password
        WebElement password = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='password']")));
        password.sendKeys("Password1234");

        // Click on Signup button to check error

        // Enter phone number
        WebElement phone = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@placeholder='Phone number']")));
        phone.sendKeys("7911 123456");

        WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='chkcondition']")));
        // scroll down to end and fill the checkbox
        js.executeScript("arguments[0].scrollIntoView();", checkbox);
        js.executeScript("arguments[0].click();", checkbox);

        WebElement signup = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='myBtn']")));
        js.executeScript("arguments[0].click();", signup);


        // Click on link from confirmation text window
        WebElement confirmation = wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("https://portal-test.homeapphub.com/Anonymous/Subscribe/roofwrightlite/")));
        js.executeScript("arguments[0].click();", confirmation);

        // Click on 'Pay by card' button
        WebElement paybycard = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@class='btn btn-link']")));
        js.executeScript("arguments[0].click();", paybycard);


    }


    @Test(priority = 2)
    void readExcel() throws IOException, InterruptedException {


        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(20))
                .pollingEvery(Duration.ofSeconds(2))
                .ignoring(NoSuchElementException.class);

        String ExcelPath = "C:\\Users\\Admin\\IdeaProjects\\Datadriven\\TestData\\TestData2.xlsx";
        String SheetName = "Sheet1";

        try (FileInputStream fileInputStream = new FileInputStream(new File(ExcelPath));
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheet(SheetName);
            int rowCount = sheet.getLastRowNum();
            DataFormatter dataFormatter = new DataFormatter();
            boolean cardAccepted = false; // Flag to check if card is accepted

            for (int i = 1; i <= rowCount; i++) {
                // Reset the flag for each iteration
                cardAccepted = false;

                Row row = sheet.getRow(i);
                String cardnumber = dataFormatter.formatCellValue(row.getCell(0));
                String expiry = dataFormatter.formatCellValue(row.getCell(1));
                String cvv = dataFormatter.formatCellValue(row.getCell(2));
                String Firstname = dataFormatter.formatCellValue(row.getCell(3));
                String Lastname = dataFormatter.formatCellValue(row.getCell(4));
                String Zipcode = dataFormatter.formatCellValue(row.getCell(5));

                // Find the fields and enter data
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(0));
                WebElement card = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@placeholder='Card number']")));
                card.sendKeys(cardnumber);
                WebElement expiry1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@placeholder='MM / YY']")));
                expiry1.sendKeys(expiry);
                WebElement cvv1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@placeholder='CVV']")));
                cvv1.sendKeys(cvv);

                // Switch to default content
                driver.switchTo().defaultContent();

                WebElement firstname = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@placeholder='First name']")));
                firstname.sendKeys(Firstname);

                // Enter last name in field
                WebElement lastname = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@placeholder='Last name']")));
                lastname.sendKeys(Lastname);

                // Enter zipcode
                WebElement zipcode = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@placeholder='Postal code / ZIP Code']")));
                zipcode.sendKeys(Zipcode);

                // Click on subscribe button
                WebElement subscribe = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='subscribeBtn']")));
                subscribe.click();

                // Wait for the page to load after clicking "Pay by card" button
                Thread.sleep(5000);

                // Check if the validation was successful
                // Check if the link "Click here to visit your subscription portal now" is present
                if (isElementPresent(By.linkText("Click here to visit your subscription portal now"))) {
                    // Card acceptance was successful, set the flag to true and break the loop
                    cardAccepted = true;
                    break;
                } else {
                    // Card acceptance failed, try the "Pay by card" button and continue the loop
                    try {
                        Thread.sleep(3000);
                        WebElement paybycard = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@class='btn btn-link']")));
                        paybycard.click();

                        Thread.sleep(2500);
                        driver.navigate().refresh();  // refresh the page because of elements settlement
                    } catch (NoSuchElementException exception) {
                        System.out.println("Validation failed for data: " + cardnumber + " - " + expiry + " - " + cvv);
                    }
                }

            }

            if (cardAccepted) {
                System.out.println("Card was accepted successfully!");
                // Perform other tasks as needed after successful validation
            } else {
                System.out.println("Card acceptance failed for all data.");
                // Handle the scenario where card acceptance failed for all rows in the Excel sheet
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Helper method to check if an element is present on the page
    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}



