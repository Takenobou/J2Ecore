# J2Ecore

## Description
J2Ecore is a utility that parses Java source files and generates an Ecore model from them. Ecore is a meta-modeling standard used primarily in the Eclipse Modeling Framework (EMF), and it provides a way to describe models and generate code from them.

## Features
- Parses `.java` files in a specified directory.
- Generates an Ecore model file.
- Exports the model to the desired location.

## Requirements
- Java 8 or higher.
- Gradle for building and running the project.

## Installation and Setup
Clone the repository to your local machine using:
```bash
git clone <repository-url>
```
Navigate to the project directory:
```bash
cd <project-directory>
```
Build the project using Gradle:
```bash
./gradlew build
```
## Usage
To run J2Ecore, execute the following command:
```bash
./gradlew run
```
Follow the prompts in the console to provide the directory path for the .java files and the output path for the .ecore file.

### Input
- The directory path where your Java files are located.
### Output
- The file path where the Ecore model will be saved.
