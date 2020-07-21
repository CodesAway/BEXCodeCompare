![BEX Code Compare](https://codesaway.info/images/BEXCodeCompare.png)

# BEX Code Compare
BEX Code Compare is a library and Eclipse plugin, which uses Patience sort / Myers diff and some post diff processing to yield an enhanced code compare

The library has no dependency on Eclipse and can be used in commercial or private projects.

 - [Maven Dependency](#maven-dependency)
 - [Screenshots of Eclipse plugin](#screenshots)
 - [What is BEX Code Compare?](#what)
 - [What is BECR?](#becr)
 - [How does it work?](#how)
 - [How does the Eclipse plugin work?](#how-plugin)
 - [Eclipse Update Site](#update-site)
 - [Installation](#install)

<a name="maven-dependency"></a>
## Maven Dependency

    <dependency>
        <groupId>info.codesaway</groupId>
        <artifactId>bex</artifactId>
        <version>0.4</version>
    </dependency>
    <dependency>
        <groupId>info.codesaway</groupId>
        <artifactId>becr</artifactId>
        <version>0.4</version>
    </dependency>
    
    <dependency>
        <groupId>info.codesaway</groupId>
        <artifactId>becr.examples</artifactId>
        <version>0.2</version>
    </dependency>

<a name="screenshots"></a>
## Screenshots of Eclipse plugin
Select BEX Code Compare  
![Select BEX Code Compare](/Screenshots/Select%20BEX%20Code%20Compare.png)

BEX Code Compare View  
![BEX Code Compare View](/Screenshots/BEX%20Code%20Compare%20View.png)

Use enhanced compare (enabled by default)  
![Use enhanced compare setting](/Screenshots/Use%20enhanced%20compare%20setting.png)

Ignore comments (disabled by default)  
![Ignore comments setting](/Screenshots/Ignore%20comments%20setting.png)

Show both sides of substitution in BEX View (disabled by default)  
![Show both sides of substitution setting in BEX View](/Screenshots/Show%20both%20sides%20of%20substitution%20setting.png)

<a name="what"></a>
## What is BEX Code Compare?
BEX Code Compare is **B**e **E**nhanced **ϽC** Code Compare

<a name="becr"></a>
## What is BECR?
BECR, pronounced Beccer, is **B**e **E**nhanced **C**ode **R**efactoring

<a name="how"></a>
## How does it work?

First we use Patience sort / Myers diff (similar to how GitHub does a compare). Then, we take the resulting diff and do some post diff processing to group the differences into groups of changes.

BECR uses BEX and Eclipe's JDT to parse Java code and provide parsing and refactoring functionality. There are examples available to show how BECR could be used.

<a name="how-plugin"></a>
## How does the Eclipse plugin work?

* We start with the BEX library and the functionality it provides outside of an IDE
* Then, the BEX Eclipse plugin identifies important versus non important changes (such as a line being split across multiple lines if ignoring whitespace differences)
* Finally, BEX shows an Eclipse view with the changes

<a name="update-site"></a>
## Update Site
[codesaway.info/eclipse](https://codesaway.info/eclipse)

### Stable
* **Name**: BEX Code Compare
* **Location**: https://codesaway.info/eclipse
* **Version**: 0.3.0

### Beta
* **Name**: BEX Code Compare
* **Location**: https://codesaway.info/eclipse-beta
* **Version**: 0.3.0

<a name="install"></a>
## Installation
1. Open Eclipse and under the **Help** menu, select **Install New Software...**
2. Click **Add...** to add a new site
   * Stable
      * **Name**: BEX Code Compare
      * **Location**: https://codesaway.info/eclipse
      * Click **Add**
   * Beta
      * **Name**: BEX Code Compare (beta)
      * **Location**: https://codesaway.info/eclipse-beta
      * Click **Add**
3. Check the box next to BEX Code Compare to install the plugin
4. Click **Next**
5. Accept the license agreement and click **Finish**
6. When prompted, install the plugin even though it's not digitally signed
7. Restart Eclipse when asked
8. Start your enhanced compare! ![BEX Code Compare](https://codesaway.info/images/BEX@2x.png)
   * When you do a compare on Java files, select BEX Code Compare from the dropdown of available compare editors
