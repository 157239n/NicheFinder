# NicheFinder v1.0

A Java application that extracts university data from https://www.niche.com/

This will automatically search for universities on Niche and grab their data so that you can analyze them later on if you want to.

## How to use it

### Go to the releases page and download the NicheFinder.jar file

![](https://i.imgur.com/z4YyOUC.png)

In the assets section, click on NicheFinder.jar and the file should download right away.

### Create a dedicated folder and put the NicheFinder.jar file there

Simple enough:

![](https://i.imgur.com/OH5E3dQ.png)

### Setting up ChromeDriver

Download ChromeDriver at http://chromedriver.chromium.org/downloads. 

At the time of making this, the latest version is 2.41:

![](https://i.imgur.com/dazkAUn.png?1)

Click on the ChromeDriver 2.41 link (or whatever the lastest version is) and this shows up:

![](https://i.imgur.com/RnBpV5Y.png)

Click on the specific file for your Operating System. It should download automatically.

You will get a .zip file. Unzip it and you will get a `chromedriver.exe` file (for Windows) and `chromedriver` file (for Mac). We currently don't support Linux or any other Operating Systems.

Move the chromedriver file into the dedicated folder. Make sure that the file is either "chromedriver.exe" or "chromedriver". It should look like this:

![](https://i.imgur.com/fIqXyAl.png)

### (Optional) Specify your account

Create a credentials.txt file inside the folder and write in your Niche account information in. It should contain 2 lines, the first line containing your email address and the second containing your password. If you do not specify this then a default account with an email "nicheexample@gmail.com" and a password of "sleepingatlast" will be used:

![](https://i.imgur.com/2ZGT1Bc.png)

Please note that the purpose of an account is to see the odds of you getting in based on your scores. The default account has scores which are based on a friend of mine: 1480 SAT (790 Math, 690 English) and 3.5 GPA. If you want to see the odds based on your scores, you will need to create a Niche account (which is super duper easy) and then specify your credentials. Don't worry, we don't store your password. The source code here proves it.

Also note that this program will delete every added school on Niche that you have already have. A bit annoying, but not too terrible.

### Specify a list of schools

Create a schools.txt file inside the folder and write in a bunch of school names. This will be used to fill out the search query on Niche itself. Note that the clearer the school name is, the more likely it is to return a result you intended:

![](https://i.imgur.com/lvcNGz5.png)

Notice that I intentionally mispelled "Massachussets" but it still works just fine. Niche will discover and correct the mistake anyway

### Execute

Double click on the jar file. Now it should open up an entirely new Chrome window and will automatically retrieve information:

![](https://i.imgur.com/UQfCitk.png)

The results will be stored under the JSON format and will be exported as a results.json file.

From now on, you can use the JSON file for all of your data analysis tools. JSON files are text files, meaning any text editor can open it. If you want to see the structure right off the bat then you can paste the JSON text inside results.json into [this website](http://jsonviewer.stack.hu/).

If you want a little bit more feed back then you can run the command `java -jar NicheFinder.jar`. If you don't want to see the browser automation process plays out then you can run `java -jar NicheFinder.jar false`

Feel free to use and fork this on https://github.com/157239n/NicheFinder. Any issues or pull requests are welcomed.
