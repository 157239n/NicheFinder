package com.n157239.niche_finder;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.util.*;

public class Main {
    private static String email = "nicheexample@gmail.com";
    private static String password = "sleepingatlast";

    private static String[] listOfTags = {"div", "span", "br"};//list of tags for method removeTag(String)
    private static final boolean debugMode = true;//if this is true then this will print extra information
    private static final int clickDelay = 1000;//delay after clicking something, in milliseconds
    private static final int textDelay = 3000;//delay after typing into something, in milliseconds
    private static final int waitForPageToLoadDelay = 3000;//delay after loading a completely new page, in milliseconds. Note that you don't need to delay at all after you use driver.get(String). This is for button pressing, loads another page type of stuff.

    //file names
    private static final String CREDENTIALS_FILE = "credentials.txt";
    private static final String SCHOOLS_FILE = "schools.txt";

    private enum OS {Windows, OSX, Other}

    @SuppressWarnings({"SpellCheckingInspection", "ReturnInsideFinallyBlock"})
    public static void main(String[] args) {
        //description
        System.out.println("Please put the chrome driver next to this jar file together in a folder in order for it to work. On windows, the file should look like chromedriver.exe, on osx, the file should look like chromedriver. You can download ChromeDriver for free at http://chromedriver.chromium.org/downloads. If you need further instructions, please visit https://github.com/157239n/NicheFinder");

        //setting up the OS-compatible chromedriver
        OS os = Environment.getOS();
        if (os == OS.Windows) {
            try {
                System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
            } catch (RuntimeException e) {
                System.out.println("Windows ChromeDriver not found");
            }
        } else if (os == OS.OSX) {
            try {
                System.setProperty("webdriver.chrome.driver", "chromedriver");
            } catch (RuntimeException e) {
                System.out.println("Mac ChromeDriver not found");
            }
        } else {
            System.out.println("Can't work on operating systems other than Windows and Mac");
            return;
        }

        //choosing whether to pop up or not
        ChromeOptions chromeOptions = new ChromeOptions();
        boolean popUpWindow = true;
        if (args.length > 0) {
            if (args[0].equals("false")) {
                popUpWindow = false;
            }
        }
        chromeOptions.setHeadless(!popUpWindow);
        WebDriver driver = new ChromeDriver(chromeOptions);

        Files.getCredentials();

        //logging into the Niche page with the specified
        if (!Niche.login(driver, email, password)) {
            System.out.println("Can't login, a problem occured");//TODO: make it so that it detects whether the credentials are correct or not
            return;
        }

        //String[] schools = {"Massachussets Institute of Technology", "Stanford University", "Tufts University"};// "massachusetts" intended to be spelled wrong. It still works though.
        if (!Niche.removePastSchools(driver)) {//we need to delete the list of schools because niche will retain the schools we added to our account and because we choose the latest school we have added, some times we might not get the school we are after.
            //// Please note that because of this, you shouldn't probably use your own account and use the nicheexample@gmail.com I have created for you guys already.
            throw new AssertionError("Failed to delete past schools");
        }

        //automate and generate results
        ArrayList<String> schools = Files.getSchools();
        StringBuilder stringBuilder = new StringBuilder("[");
        for (int i = 0; i < schools.size(); i++) {
            HashMap<String, String> values = Niche.exploreSchoolGivenURL(driver, Niche.getSchoolHTML(driver, schools.get(i)));
            USNews.exploreSchoolGivenURL(driver, USNews.getSchoolHTML(driver, schools.get(i)), values);
            System.out.println("----------------------------------------------------------------------");
            ArrayList<String> valuesInArrayList = Environment.generateStringArrayListFromHashMap(values);
            valuesInArrayList.sort(Comparator.naturalOrder());
            Environment.printArrayList(valuesInArrayList);
            stringBuilder.append(Environment.generateJSONFromHashMap(values));
            if (i + 1 < schools.size()) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append("]");

        //write result onto a JSON file
        try (PrintWriter printWriter = new PrintWriter(new FileOutputStream("results.json"))) {
            printWriter.write(stringBuilder.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        driver.quit();
    }

    private static class Files {
        /**
         * Reads from schools.txt file and look for a list of schools.
         *
         * @return a String ArrayList containing the list of schools to search for
         */
        private static ArrayList<String> getSchools() {
            //getting the schools name from schools.txt
            BufferedReader reader = null;
            ArrayList<String> schools = new ArrayList<>();
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(SCHOOLS_FILE)));
                String newSchool = reader.readLine().trim();
                while (!newSchool.equals("")) {
                    schools.add(newSchool);
                    newSchool = reader.readLine();
                }
            } catch (RuntimeException | IOException e) {
                //use the defaults
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (IOException e) {
                    System.out.println("Can't close file " + SCHOOLS_FILE);
                }
            }
            return schools;
        }

        /**
         * Reads from credentials.txt 2 lines of text and look for an email and a password. If found then sets the global variable "email" and "password"
         */
        private static void getCredentials() {
            //getting the account from credentials.txt
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(CREDENTIALS_FILE)));
                String newEmail = reader.readLine();
                String newPassword = reader.readLine();
                if (newEmail == null || newPassword == null) {
                    System.out.println("No credentials found, using default account...");
                    throw new RuntimeException();
                }
                if (newEmail.equals("")) {
                    System.out.println("Email is blank!");
                    throw new RuntimeException();
                }
                if (newPassword.equals("")) {
                    System.out.println("Password is blank!");
                    throw new RuntimeException();
                }
                email = newEmail;
                password = newPassword;
            } catch (RuntimeException | IOException e) {
                //use the defaults
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    System.out.println("Can't close file " + CREDENTIALS_FILE);
                }
            }
        }
    }

    private static class Niche {
        /**
         * Logs in to a Niche account using a specified driver, an email and a password. Waits 3 seconds after logged in.
         *
         * @param driver   the WebDriver object
         * @param email    the email
         * @param password the password
         * @return whether the login operation was successful or not
         */
        @SuppressWarnings("SameParameterValue")
        private static boolean login(WebDriver driver, String email, String password) {
            try {
                driver.get("https://www.niche.com/account/login/");
                //logging in
                {
                    WebElement emailBox = driver.findElement(By.name("login-email"));
                    if (debugMode) System.out.println("EmailBox tag name: " + emailBox.getTagName());
                    emailBox.sendKeys(email);

                    WebElement passwordBox = driver.findElement(By.name("login-password"));
                    if (debugMode) System.out.println("PasswordBox tag name: " + passwordBox.getTagName());
                    passwordBox.sendKeys(password);

                    WebElement submitButton = driver.findElement(By.className("form__submit__btn"));
                    if (debugMode) System.out.println("submitButton tag name: " + submitButton.getTagName());
                    submitButton.click();

                    if (debugMode) System.out.println("Page title is: " + driver.getTitle());
                }
                Environment.sleep(waitForPageToLoadDelay);
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }

        /**
         * Removes past schools and starts with a new list of schools. This assumes that you have already signed in. Waits 1 second when it has done deleting.
         *
         * @return whether the operation was successful or not
         */
        private static boolean removePastSchools(WebDriver driver) {
            try {
                //entity-card__actions__remove
                driver.get("https://www.niche.com/account/");
                WebElement divListOfSchools = driver.findElement(By.className("entity-card-list"));
                List<WebElement> listOfSchools = divListOfSchools.findElements(By.className("card--compact"));
                for (WebElement listOfSchool : listOfSchools) {
                    try {
                        listOfSchool.findElement(By.className("entity-card__actions__remove")).click();
                    } catch (RuntimeException e) {
                        // if something breaks then refresh and tries again
                        return removePastSchools(driver);
                    }
                    Environment.sleep(clickDelay);
                }
                return true;
            } catch (RuntimeException e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * Looks for a school and then extracts information out of it. This assumes that you have already logged in. Does not wait after finished.
         *
         * @param schoolName the name of the school we are looking for
         * @return the URL of the page specific to the school, empty ("") if anything goes wrong along the way
         */
        @SuppressWarnings("SameParameterValue")
        private static String getSchoolHTML(WebDriver driver, String schoolName) {
            try {
                driver.get("https://www.niche.com/account/");
                System.out.println("Page title is: " + driver.getTitle());
                // fills in the query box
                {
                    List<WebElement> elements = driver.findElements(By.tagName("input"));
                    if (debugMode) System.out.println("Number of elements are: " + elements.size());
                    WebElement element = elements.get(6);

                    if (debugMode) System.out.println("Search box text: " + element.getAttribute("placeholder"));
                    if (debugMode) System.out.println("Search box displayed?: " + element.isDisplayed());

                    element.sendKeys(schoolName);
                    Environment.sleep(textDelay);
                    //sometimes when it types too fast, Niche only recognize a part of the query only and thus other schools might pop in instead of the one we really want to search
                    element.sendKeys(" ");
                    Environment.sleep(textDelay);
                }
                // selects the first school and then selects the background to de-focus
                {
                    List<WebElement> listOfUlListOfSchools = driver.findElements(By.className("sherlock__results"));

                    WebElement ulListOfSchools = null;
                    for (WebElement listOfUlListOfSchool : listOfUlListOfSchools) {
                        if (listOfUlListOfSchool.isDisplayed()) {
                            ulListOfSchools = listOfUlListOfSchool;
                            break;
                        }
                    }

                    if (ulListOfSchools == null) {
                        System.out.println("Can't find school \"" + schoolName + "\" on Niche");
                        return "";
                    }

                    // find the query results
                    List<WebElement> listOfSchools = ulListOfSchools.findElements(By.tagName("li"));
                    if (debugMode) System.out.println("Number of schools found: " + listOfSchools.size());

                    WebElement firstResultFound = listOfSchools.get(0);
                    if (debugMode)
                        System.out.println("School chosen: " + Environment.removeTag(firstResultFound.findElement(By.tagName("span")).findElement(By.tagName("span")).getAttribute("innerHTML"), "strong"));
                    if (debugMode) System.out.println("School is visible: " + firstResultFound.isDisplayed());

                    // click the first school found and add it to my list of schools
                    try {
                        firstResultFound.click();
                    } catch (ElementNotVisibleException e) {
                        ulListOfSchools.click();
                    }
                    Environment.sleep(clickDelay);

                    // click the background of the page so that it eliminates the focus of the search bar
                    driver.findElement(By.className("account-hello")).click();
                    Environment.sleep(clickDelay);
                }
                // clicks to view more about the school
                {
                    List<WebElement> listOfSchools = driver.findElements(By.className("entity-card-list")).get(0).findElements(By.className("card--compact"));
                    if (listOfSchools.size() == 0) {
                        return "";
                    }
                    return listOfSchools.get(0).findElement(By.className("entity-card__actions__profile")).getAttribute("href");
                }
            } catch (RuntimeException e) {
                return "";
            }
        }

        /**
         * Explore a specific school when given the URL of the Niche page of that school.
         *
         * @param driver    the WebDriver
         * @param schoolURL the school's Niche URL
         * @return a HashMap containing the information of the schools
         */
        private static HashMap<String, String> exploreSchoolGivenURL(WebDriver driver, String schoolURL) {
            return exploreSchoolGivenURL(driver, schoolURL, new HashMap<>());
        }

        /**
         * Explore a specific school when given the URL of the Niche page of that school and an existing HashMap.
         *
         * @param driver          the WebDriver
         * @param schoolURL       the school's Niche URL
         * @param existingHashMap the existing HashMap if we have any
         * @return a HashMap containing the information of the schools
         */
        private static HashMap<String, String> exploreSchoolGivenURL(WebDriver driver, String schoolURL, HashMap<String, String> existingHashMap) {
            if (schoolURL.equals("")) return new HashMap<>();
            try {
                driver.get(schoolURL);
                existingHashMap.put("(Niche A) School name", driver.findElement(By.className("profile-entity-name__link")).getAttribute("innerHTML"));

                try {
                    WebElement reportCard = driver.findElement(By.id("report-card"));
                    existingHashMap.put("(Niche B) Overall grade", reportCard.findElement(By.className("overall-grade__niche-grade")).findElement(By.tagName("div")).getAttribute("innerHTML"));
                } catch (RuntimeException e) {
                    //in case this one fails
                }

                try {
                    WebElement about = driver.findElement(By.id("about"));
                    existingHashMap.put("(Niche C1) Type", about.findElement(By.className("profile__bucket--1")).findElements(By.className("scalar__value")).get(0).findElement(By.tagName("span")).getAttribute("innerHTML"));
                    existingHashMap.put("(Niche C2) Address", Environment.removeTag(about.findElement(By.className("profile__bucket--2")).findElement(By.className("profile__address")).findElements(By.tagName("div")).get(1).getAttribute("innerHTML")));
                    existingHashMap.put("(Niche C3) Website", Environment.removeTag(about.findElement(By.className("profile__bucket--2")).findElement(By.className("profile__website")).findElements(By.tagName("div")).get(1).findElement(By.tagName("a")).getAttribute("href")));
                } catch (RuntimeException e) {
                    //in case this one fails
                }

                try {
                    WebElement admissions = driver.findElement(By.id("admissions"));
                    existingHashMap.put("(Niche D1) Acceptance rate", Environment.removeTag(admissions.findElement(By.className("profile__bucket--1")).findElement(By.className("scalar__value")).findElement(By.tagName("span")).getAttribute("innerHTML")));
                    existingHashMap.put("(Niche D2) SAT range", Environment.removeTag(admissions.findElement(By.className("profile__bucket--3")).findElements(By.className("scalar__value")).get(0).findElement(By.tagName("span")).getAttribute("innerHTML")));
                    existingHashMap.put("(Niche D3) ACT range", Environment.removeTag(admissions.findElement(By.className("profile__bucket--3")).findElements(By.className("scalar__value")).get(1).findElement(By.tagName("span")).getAttribute("innerHTML")));
                    existingHashMap.put("(Niche D4) Application fee", Environment.removeTag(admissions.findElement(By.className("profile__bucket--3")).findElements(By.className("scalar__value")).get(2).findElement(By.tagName("span")).getAttribute("innerHTML")));
                } catch (RuntimeException e) {
                    //in case this one fails
                }

                try {
                    WebElement scatterplot = driver.findElement(By.id("scatterplot"));
                    scatterplot.click();
                    Environment.sleep(2000);
                    existingHashMap.put("(Niche E1) Rank higher than", scatterplot.findElement(By.className("scatterplot__percentile-text")).findElement(By.tagName("em")).getAttribute("innerHTML"));
                } catch (RuntimeException e) {
                    //in case this one fails
                }

                try {
                    WebElement students = driver.findElement(By.id("students"));
                    existingHashMap.put("(Niche F1) Number of students", students.findElement(By.className("profile__bucket--1")).findElements(By.className("scalar__value")).get(0).findElement(By.tagName("span")).getAttribute("innerHTML"));
                } catch (RuntimeException e) {
                    //in case this one fails
                }

                try {
                    WebElement after = driver.findElement(By.id("after"));
                    existingHashMap.put("(Niche G1) Earning after 6 years", after.findElement(By.className("profile__bucket--1")).findElements(By.className("scalar__value")).get(0).findElement(By.tagName("span")).getAttribute("innerHTML"));
                } catch (RuntimeException e) {
                    //in case this one fails
                }

                try {//go deeper into costs
                    driver.get(schoolURL + "/cost/");

                    try {
                        WebElement netPrice = driver.findElement(By.id("net-price"));
                        existingHashMap.put("(Niche H1) Net price", netPrice.findElement(By.className("profile__bucket--1")).findElement(By.className("scalar__value")).findElement(By.tagName("span")).getAttribute("innerHTML"));
                        existingHashMap.put("(Niche H2) Net price calculator", netPrice.findElement(By.className("profile__bucket--1")).findElement(By.className("profile__website")).findElement(By.tagName("a")).getAttribute("href"));
                    } catch (RuntimeException e) {
                        //in case this one fails
                    }

                    try {
                        WebElement stickerPrice = driver.findElement(By.id("sticker-price"));
                        existingHashMap.put("(Niche I1) In-State Tuition", stickerPrice.findElement(By.className("profile__bucket--1")).findElement(By.className("scalar__value")).findElement(By.tagName("span")).getAttribute("innerHTML"));
                        existingHashMap.put("(Niche I2) Out-of-State Tuition", stickerPrice.findElement(By.className("profile__bucket--2")).findElement(By.className("scalar__value")).findElement(By.tagName("span")).getAttribute("innerHTML"));
                    } catch (RuntimeException e) {
                        //in case this one fails
                    }

                    try {
                        WebElement overallValue = driver.findElement(By.id("overall-value"));
                        existingHashMap.put("(Niche J1) Overall value", overallValue.findElement(By.className("niche__grade")).getAttribute("innerHTML"));
                    } catch (RuntimeException e) {
                        //in case this one fails
                    }
                } catch (RuntimeException e) {
                    //in case this one fails
                }
                return existingHashMap;
            } catch (RuntimeException e) {
                return new HashMap<>();
            }
        }
    }

    private static class USNews {
        private static String getSchoolHTML(WebDriver driver, String schoolName) {
            try {
                schoolName = schoolName.replace(" ", "%20");
                driver.get("https://www.usnews.com/best-colleges/search?school-name=" + schoolName);
                if (debugMode) System.out.println("Page title is: " + driver.getTitle());

                // selects the first school
                WebElement ulListOfSchools = driver.findElement(By.id("search-application-results-view"));

                // find the query results
                List<WebElement> listOfSchools = ulListOfSchools.findElements(By.tagName("li"));
                if (debugMode) System.out.println("Number of schools found: " + listOfSchools.size());

                WebElement firstResultFound = listOfSchools.get(0).findElement(By.tagName("section")).findElement(By.tagName("div")).findElement(By.tagName("h3")).findElement(By.tagName("a"));
                if (debugMode) System.out.println("School chosen: " + firstResultFound.getAttribute("innerHTML"));
                return firstResultFound.getAttribute("href");
            } catch (RuntimeException e) {
                return "";
            }
        }

        /**
         * Explore a specific school when given the URL of the Niche page of that school.
         *
         * @param driver    the WebDriver
         * @param schoolURL the school's Niche URL
         * @return a HashMap containing the information of the schools
         */
        @SuppressWarnings("unused")
        private static HashMap<String, String> exploreSchoolGivenURL(WebDriver driver, String schoolURL) {
            return exploreSchoolGivenURL(driver, schoolURL, new HashMap<>());
        }

        /**
         * Explore a specific school when given the URL of the Niche page of that school and an existing HashMap.
         *
         * @param driver          the WebDriver
         * @param schoolURL       the school's Niche URL
         * @param existingHashMap the existing HashMap if we have any
         * @return a HashMap containing the information of the schools
         */
        private static HashMap<String, String> exploreSchoolGivenURL(WebDriver driver, String schoolURL, HashMap<String, String> existingHashMap) {
            if (schoolURL.equals("")) return new HashMap<>();
            try {
                driver.get(schoolURL);
                if (debugMode) System.out.println("Waiting for popup window to open");
                Environment.sleep(waitForPageToLoadDelay);
                try {
                    //if there is a pop-up window then close it
                    driver.findElement(By.className("close-reveal-modal")).click();
                    Environment.sleep(clickDelay);
                    if (debugMode) System.out.println("There is a window popped up");
                } catch (RuntimeException e) {
                    //in case there is no close button because no window ever pop up in the first place
                    if (debugMode)
                        System.out.println("There isn't a window popped up or it can't detect that there is one");
                }

                try {
                    existingHashMap.put("(US News A1) School name", driver.findElement(By.className("hero-heading")).getAttribute("innerHTML").trim());
                    existingHashMap.put("(US News A2) School address", driver.findElement(By.className("hero-ranking-data-contact")).getText());
                } catch (RuntimeException e) {
                    //in case this one fails
                }

                try {//getting the rank and overall score
                    WebElement rankingBox = driver.findElement(By.className("hero-ranking-data-rank"));
                    if (rankingBox.getAttribute("innerHTML").contains("Unranked")) {
                        existingHashMap.put("(US News B1) Rank", "Unranked");
                    } else {
                        WebElement rank = rankingBox.findElement(By.tagName("strong"));//like "#3"
                        WebElement type = rankingBox.findElement(By.tagName("a"));//like "National Universities"
                        existingHashMap.put("(US News B1) Rank", rank.getText() + " " + type.getAttribute("innerHTML"));
                        WebElement score = rankingBox.findElement(By.className("hide-for-small-only")).findElement(By.tagName("span"));//like 51/100.0
                        existingHashMap.put("(US News B2) Overall score", score.getAttribute("innerHTML") + "/100");
                    }
                } catch (RuntimeException e) {
                    //in case this one fails
                }

                try {
                    driver.get(schoolURL + "/paying");
                    if (debugMode) System.out.println("Waiting for popup window to open");
                    Environment.sleep(waitForPageToLoadDelay);
                    try {
                        //if there is a pop-up window then close it
                        driver.findElement(By.className("close-reveal-modal")).click();
                        Environment.sleep(clickDelay);
                        if (debugMode) System.out.println("There is a window popped up");
                    } catch (RuntimeException e) {
                        //in case there is no close button because no window ever pop up in the first place
                        if (debugMode)
                            System.out.println("There isn't a window popped up or it can't detect that there is one");
                    }
                    WebElement finaid = driver.findElement(By.className("fullwidth-accordion")).findElements(By.className("accordion-navigation")).get(2);
                    finaid.click();
                    Environment.sleep(clickDelay);

                    WebElement chart = finaid.findElement(By.id("FinancialAid")).findElement(By.tagName("div")).findElement(By.className("data-dictionary-field")).findElement(By.tagName("div")).findElements(By.tagName("div")).get(1).findElement(By.tagName("div"));
                    List<WebElement> chartLines = chart.findElement(By.className("paper-eagle-y-axis")).findElement(By.className("paper-eagle-grid")).findElements(By.tagName("line"));
                    //it is expected that the 0% line has a higher value than the 100% line because in software, the origin is at the top left corner
                    float line0 = Float.parseFloat(chartLines.get(0).getAttribute("y1"));
                    float line100 = Float.parseFloat(chartLines.get(4).getAttribute("y1"));

                    List<WebElement> barLines = chart.findElement(By.className("chart-area")).findElements(By.tagName("line"));
                    if (barLines.size() != 5) {
                        System.out.println("Financial aid information on US News has changed and thus can't fetch data");
                        throw new RuntimeException();
                    }
                    ArrayList<Float> values = new ArrayList<>();
                    for (WebElement barLine : barLines) {
                        values.add(Float.parseFloat(barLine.getAttribute("y2")));
                    }
                    existingHashMap.put("(US News C1) % applied for need-based aid", String.valueOf((1 - (values.get(0) - line100) / (line0 - line100)) * 100) + "%");
                    existingHashMap.put("(US News C2) % need was fully met", String.valueOf((1 - (values.get(1) - line100) / (line0 - line100)) * 100) + "%");
                    existingHashMap.put("(US News C3) % received need-based financial aid", String.valueOf((1 - (values.get(2) - line100) / (line0 - line100)) * 100) + "%");
                    existingHashMap.put("(US News C4) % received need-based self-help aid", String.valueOf((1 - (values.get(3) - line100) / (line0 - line100)) * 100) + "%");
                    existingHashMap.put("(US News C5) % of need met on average", String.valueOf((1 - (values.get(4) - line100) / (line0 - line100)) * 100) + "%");
                } catch (RuntimeException e) {
                    //in case this one fails
                }
                return existingHashMap;
            } catch (RuntimeException e) {
                return new HashMap<>();
            }
        }
    }

    private static class Environment {
        /**
         * Sleeps for a specified amount of time. Exception when timed out is taken care of.
         *
         * @param timeInMillis the number of milliseconds to sleep
         */
        private static void sleep(int timeInMillis) {
            try {
                Thread.sleep(timeInMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (debugMode) System.out.println("Done waiting");
        }

        /**
         * Takes in a piece of html text and removes all tags of a specific kind.
         *
         * @param html the piece of html
         * @param tag  the tag to remove
         * @return the finalized html piece
         */
        private static String removeTag(String html, String tag) {
            return html.replace("<" + tag + ">", " ").replace("</" + tag + ">", " ");
        }

        /**
         * Takes in a piece of html text and remove all possible tags, specified by {@link Main#listOfTags}
         *
         * @param html the piece of html
         * @return the finalized html piece
         */
        private static String removeTag(String html) {
            String ans = html;
            for (String listOfTag : listOfTags) {
                ans = removeTag(ans, listOfTag);
            }
            return ans.replace("<!--", " ").replace("-->", "");
        }

        /**
         * Prints a HashMap onto the console.
         *
         * @param hashMap the HashMap to print
         */
        private static void printHashMap(HashMap<String, String> hashMap) {
            if (hashMap == null) {
                throw new NullPointerException();
            }
            for (Object o : hashMap.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                System.out.println(pair.getKey() + ": " + pair.getValue());
            }
        }

        private static void printArrayList(ArrayList<String> array) {
            for (String anArray : array) {
                System.out.println(anArray);
            }
        }

        private static ArrayList<String> generateStringArrayListFromHashMap(HashMap<String, String> hashMap) {
            ArrayList<String> answer = new ArrayList<>();
            Iterator it = hashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                answer.add("\"" + pair.getKey() + "\": \"" + pair.getValue() + "\"");
            }
            return answer;
        }

        /**
         * Generates a JSON text from a HashMap. Please note that this method is very simple and does not check for escape sequences.
         *
         * @param hashMap the HashMap to convert to JSON
         * @return a JSON piece of text
         */
        @SuppressWarnings("unused")
        private static String generateJSONFromHashMap(HashMap<String, String> hashMap) {
            StringBuilder ans = new StringBuilder("{");
            if (hashMap == null) {
                throw new NullPointerException();
            }
            //get all the values to an ArrayList to sort it in alphabetical order
            ArrayList<String> linesWithoutEndingComma = generateStringArrayListFromHashMap(hashMap);
            linesWithoutEndingComma.sort(Comparator.naturalOrder());
            for (int i = 0; i < linesWithoutEndingComma.size(); i++) {
                ans.append(linesWithoutEndingComma.get(i));
                if (i + 1 < linesWithoutEndingComma.size()) {
                    ans.append(", ");
                }
            }
            ans.append("}");
            return ans.toString();
        }

        /**
         * Gets the current OS.
         *
         * @return the current OS
         */
        private static OS getOS() {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                return OS.Windows;
            } else if (osName.contains("mac")) {
                return OS.OSX;
            } else {
                return OS.Other;
            }
        }
    }
}
