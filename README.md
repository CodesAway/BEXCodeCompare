![BEX Code Compare](https://codesaway.info/images/BEXCodeCompare.png)

# BEX Code Compare
BEX Code Compare is an Eclipse plugin, which uses Patience sort / Myers diff and some post diff processing to yield an enhanced code compare

## Update site
[codesaway.info/eclipse](https://codesaway.info/eclipse)

### Stable
* **Name**: BEX Code Compare
* **Location**: https://codesaway.info/eclipse
* **Version**: 0.1.2

### Beta
* **Name**: BEX Code Compare
* **Location**: https://codesaway.info/eclipse-beta
* **Version**: 0.1.2

## What is BEX Code Compare?
BEX Code Compare is **B**e **E**nhanced Code Compare

## How does it work?

First we use the Patience sort / Myers diff (similar to how GitHub does a compare). Then, we take the resulting diff and do some post diff processing to group the differences into groups of changes.

Along the way, BEX identifies important versus non important changes (such as a line being split across multiple lines if ignoring whitespace differences). It then shows an Eclipse view with the changes.

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
3. Check the box next to BEX Code COmpare to install the plugin
4. Click **Next**
5. Accept the license agreement and click **Finish**
6. When prompted, install the plugin even though it's not digitally signed
7. Restart Eclipse when asked
8. Start your enhanced compare! ![BEX Code Compare](https://codesaway.info/images/BEX.png)
   * When you do a compare on Java files, select BEX Code Compare from the dropdown of available compare editors
