---
layout: page
title: Install
permalink: /install/
---

Installing the Call Graph Toolbox Eclipse plugin is easy.  It is recommended to install the plugin from the provided update site, but it is also possible to install from source.
        
### Installing Dependencies
1. First make sure you have [Atlas](http://www.ensoftcorp.com/atlas/download/) Standard or Pro installed.
2. When installing Atlas make sure to also include Atlas Experimental features (to include Atlas for Jimple support).
        
### Installing from Update Site
Follow the steps below to install the Call Graph Toolbox plugin from the Eclipse update site.

1. Start Eclipse, then select `Help` &gt; `Install New Software`.
2. Click `Add`, in the top-right corner.
3. In the `Add Repository` dialog that appears, enter &quot;Slicing Toolbox&quot; for the `Name` and &quot;[https://ensoftcorp.github.io/slicing-toolbox/updates/](https://ensoftcorp.github.io/slicing-toolbox/updates/)&quot; for the `Location`.
4. In the `Available Software` dialog, select the checkbox next to "Points-to Toolbox" and click `Next` followed by `OK`.
5. In the next window, you'll see a list of the tools to be downloaded. Click `Next`.
6. Read and accept the license agreements, then click `Finish`. If you get a security warning saying that the authenticity or validity of the software can't be established, click `OK`.
7. When the installation completes, restart Eclipse.

## Installing from Source
If you want to install from source for bleeding edge changes, first grab a copy of the [source](https://github.com/EnSoftCorp/slicing-toolbox) repository. In the Eclipse workspace, import the `com.ensoftcorp.open.slice` Eclipse project located in the source repository.  Right click on the project and select `Export`.  Select `Plug-in Development` &gt; `Deployable plug-ins and fragments`.  Select the `Install into host. Repository:` radio box and click `Finish`.  Press `OK` for the notice about unsigned software.  Once Eclipse restarts the plugin will be installed and it is advisable to close or remove the `com.ensoftcorp.open.slice` project from the workspace.

## Changelog
Note that version numbers are based off [Atlas](http://www.ensoftcorp.com/atlas/download/) version numbers.

### 2.7.3
- Initial Release
