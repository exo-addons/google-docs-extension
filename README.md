eXo Google Docs Extension
===================

Edit an eXo Platform document directly inside Google Docs

Prerequisite : eXo users must have a valid Google account email (not necessarily a gmail account)

Getting Started
===============

Step 1 :  Build 
----------------
Prerequisite : install [Maven 3](http://maven.apache.org/download.html)

Clone the project with

    git clone https://github.com/exo-addons/google-docs-extension.git
    cd google-docs-extension

Build it with

    mvn clean package

Step 2 : Deploy 
---------------

Prerequisite : install [eXo Platform 3.5 Tomcat bundle](http://www.exoplatform.com/company/en/download-exo-platform) (EXO\_TOMCAT\_ROOT\_FOLDER will be used to designate the eXo Tomcat root folder).

Copy the extension binaries :

    cp config/target/googledocs-extension-config*.jar EXO_TOMCAT_ROOT_FOLDER/lib
    cp services/target/googledocs-extension-services*.jar EXO_TOMCAT_ROOT_FOLDER/lib
    cp webapp/target/googledocs-extension.war EXO_TOMCAT_ROOT_FOLDER/webapp

Create a file called googledocs-extension.xml in EXO\_TOMCAT\_ROOT\_FOLDER/conf/Catalina/localhost with the following content :

    <Context path="/googledocs-extension" docBase="googledocs-extension" debug="0" reloadable="true" crossContext="true"/>

Download the [Google Drive Java Client](http://code.google.com/p/google-api-java-client/wiki/APIs#Drive_API) and copy the following libs in EXO\_TOMCAT\_ROOT\_FOLDER/lib :

- google-api-services-drive-v2-rev5-1.7.2-beta.jar
- google-api-client-1.10.3-beta.jar
- google-http-client-1.10.3-beta.jar
- google-oauth-client-1.10.1-beta.jar
- gson-2.1.jar
- guava-11.0.1.jar
- httpclient-4.0.3.jar
- jackson-core-asl-1.9.4.jar
- jsr305-1.3.9.jar
- protobuf-java-2.2.0.jar


Step 3 : Enable Drive API
-------------------------

- Go to the Google API Console : https://code.google.com/apis/console/
- Create an new API project
- In the Services page, enable the Drive API

![Google Drive API](https://raw.github.com/exo-addons/google-docs-extension/master/readme-resources/google-drive-api.png)

- In the API Access page, click on the "Create an OAuth 2.0 client ID..." button
- Fill the form with a product name of your choice, an optionnally a product logo and a home page URL
- Click Next
- Select the "Service account" option
- Click on "Create client ID"
- Download the private key and save it on your eXoPlatform server's file system

![Google Drive API Access](https://raw.github.com/exo-addons/google-docs-extension/master/readme-resources/google-drive-key.png)

Step 4 : Configure the extension 
--------------------------------

- Open the file EXO\_TOMCAT\_ROOT\_FOLDER/gatein/conf/configuration.properties of your eXoPlatform server
- Add the 3 following variables :

    google-docs.serviceEmail=755138841809@developer.gserviceaccount.com
    google-docs.accountEmail=mygreatcompany@gmail.com
    google-docs.privateKeyFilePath=/path/to/my/private/key/4314ebd80f114feef1f19ad6e8b27ad144847144-privatekey.p12

The google-docs.serviceEmail parameter is the Client ID of the service account (available in your Google console, see previous screenshot).
The google-docs.accountEmail parameter is your Google account.
The google-docs.privateKeyFilePath parameter is the path to your previously downloaded private key.

Step 5 : Run
------------

    cd EXO_TOMCAT_ROOT_FOLDER 
    ./start_eXo.sh

Step 6 : Add the Google Docs buttons
------------------------------------
- Connect as an administrator
- Go to the Content Administration
- Content Presentation > Manage View
- Edit the view you want to add the buttons in, for example WCM View
- Click on the tab you want to add the buttons in, for example Publication
- Select the 2 buttons "Edit in Google Docs" and "Check In from Google Docs"

![Add buttons](https://raw.github.com/exo-addons/google-docs-extension/master/readme-resources/add-buttons.png)

- Save

User Guide
===============

- Go to the Sites Explorer
- Open a document (with a type supported by Google Docs : docx, xlsx, txt, ...). A button "Edit in Google Docs" appears in the action bar

![Edit in Google Docs](https://raw.github.com/exo-addons/google-docs-extension/master/readme-resources/edit-in-google-docs.png)

- Click on this button. The document will be uploaded to Google Docs and you will be redirected to this document in Google Docs
- Make some changes in your document

![Doc in Google Docs](https://raw.github.com/exo-addons/google-docs-extension/master/readme-resources/doc-in-google-docs.png)

- Come back to the Sites Explorer, a new button "Checkin from Google Docs" is available

![Checkin from Google Docs](https://raw.github.com/exo-addons/google-docs-extension/master/readme-resources/checkin-from-googledocs.png)

- Click on this button. Your content is now up to date in eXoPlatform !

Of course, thanks to Google Docs, several users can edit the content simultaneously !

Supported document types are :

- docx
- xlsx
- pptx
- odt
- ods
- odp
- rtf
- html
- txt
- csv
