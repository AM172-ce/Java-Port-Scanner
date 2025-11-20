# 🔍 Java Port Scanner

*A fast, lightweight, multithreaded TCP port scanner written in Java.*

This project is a foundational networking tool designed to explore TCP connectivity, service discovery, and basic protocol interaction. It is built as a standalone mini-project to exercise socket programming, concurrency, timeout handling, and clean CLI design in Java.

The portScanner supports configurable host/port ranges, parallel execution, connection timeouts, and optional banner grabbing for service identification.
It serves as an educational precursor to more advanced network scanning tools (e.g., SYN scanning, OS detection, vulnerability mapping).

---
## How to Run on Windows

##  1. Build the Fat JAR

This project uses Gradle. To build the runnable JAR:

```bash
gradle clean build
```

The fat JAR (with dependencies) will be located in:

```
build/libs/Java-Port-Scanner-0.1.SNAPSHOT.jar
```

---

## 2. Install the Scanner

Choose a folder where you want to install the scanner executable.
Example:

```
C:\scannerj
```

Copy the generated JAR into that folder:

```
Java-Port-Scanner-0.1.SNAPSHOT.jar
```

Your directory should look like:

```
C:\scannerj\
    Java-Port-Scanner-0.1.SNAPSHOT.jar
    scannerj.bat
```

---

## 3. Create the Launcher Script (`scannerj.bat`)

Create a file named **scannerj.bat** in the same directory:

```bash
@echo off
rem locate java on PATH and run jar with forwarded args
java -jar "%~dp0\Java-Port-Scanner-0.1.SNAPSHOT.jar" %*
```

This script:

* Automatically finds the JAR next to it
* Forwards any arguments to your scanner
* Does not require `cd`ing into the folder where the JAR is

---

## 4. Add scannerj to the System PATH

So you can run it from **any directory**, add the install folder to PATH:

1. Open **System Properties → Advanced → Environment Variables**
2. Under **System variables**, find: `Path`
3. Click **Edit**
4. Click **New**
5. Add:

```
C:\scannerj
```

6. Click **OK** everywhere

To verify PATH is updated:

```powershell
where scannerj
```

You should see:

```
C:\scannerj\scannerj.bat
```

---

## 5. Run the Scanner

Now you can run the tool from *any directory*:

```
scannerj <host(s)-ip(s)/host(s)-name(s)/host-with-cidr> -p <port/ports/top-ports>
```

### Examples:

```bash
scannerj 192.168.1.1 -p 80
scannerj example.com -p 1-1000
scannerj 192.168.1.0/24 --top-ports 100
```

Arguments are passed directly to the JAR through the batch file.

---
## Updating the Tool

When you rebuild new versions of the scanner:

1. Replace the JAR inside `C:\scannerj`
2. Keep the same script name `scannerj.bat`
3. PATH stays the same, nothing else to do

---
