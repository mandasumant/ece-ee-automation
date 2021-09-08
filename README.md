# Autodesk ECE EE Automation Project

This Repo will be used for Customer Journey Automation for ECE scoped services.

## Description

The ece ee automation project includes validation for:

- Checkout workflow with:
    - BiC & Meta subscription
    - Promotion Code
    - Flex, Cloud credits...
    - Add/Reduce seat workflow
    - Trial Downloads workflow
    - Change payment method (Billing manager)
    - Switch terms
    - Renewal
    - Align billing
    - Manual Order Entry
- Commerce workflow*:
    - BiC subscription</br>
      Note: *We aren't validating subscription in portal.

## Project Setup

This project requires Java and Maven to be installed/configured.

### Download/Install Java JDK

```
Go to: https://www.oracle.com/java/technologies/javase-downloads.html
```

### Download/Install Maven

```
Go to: https://maven.apache.org/install.html
```

### Update bash profile file

From your terminal, run the following command:
```bash
open ~/.bash_profile
```
Add the following lines if not done already:
```
i.e.: export M2_REPO=/Users/[user]/.m2
i.e.: export JAVA_HOME=/Library/Java/JavaVirtualMachines/[yourversion]/Contents/Home
```

Note: system restart might be needed to be recognized by the system.

### Clone ece ee automation git repo

1. From your terminal `cd ...` into a desire location
2. Run the following command line:
    ```bash
    git clone https://git.autodesk.com/dpe/ece-ee-automation.git
    ```

### Maven Profile

The required settings.xml file is already located at the root of this project.

Update the following part:

```xml

<server>
  <username>[USERNAME]</username>
  <password>[PASSWORD]</password>
  <id>Autodeskcentral</id>
</server>
```

- `USERNAME` - Ask ece automation team for the username to be added.
- `PASSWORD` - Ask ece automation team for the password to be added.
- `MAVEN_REPO` - The absolute path of your maven repository folder (
  i.e.: `~/.m2/repo-ece-ee-automation`). <br /> Note: Do not use the home directory tilde character in
  your path.

## Build

Build the project without test:

```bash
mvn clean install -DskipTests
```

## Running your first test

Tests are initiated through bash command line.
See [wiki](https://wiki.autodesk.com/display/EFDE/Automation+Command+Line) for reference:

## Formatting guideline

Before contributing to this repo, please review our formatting guideline
policy [here](https://wiki.autodesk.com/display/EFDE/Project+formatter+and+checkstyle).
