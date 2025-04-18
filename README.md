# B2XKlaim


  <img src="https://github.com/PROSLab/B2XKlaim/blob/main/b2xklaim.jpg" width="500" height="auto">



`B2XKlaim` is a powerful tool designed to transform Collaboration diagrams from BPMN into skeleton code for Xklaim, an innovative programming language rooted in the formal language KLAIM.

![B2XKlaim Logo](https://github.com/khalidbourr/B2XKlaim/blob/main/Screenshot%20from%202025-02-26%2022-10-16.png)
---

## Table of Contents

- [Features](#features)
- [Background](#background)
- [Installation](#installation)
- [Usage](#usage)

---

## Features

- **Code Generation**: Automatically produce Xklaim skeleton code from BPMN Collaboration diagrams.
- **Support for BPMN Elements**: Seamless integration of XOR, AND, Start, Intermediate, and End events. Additionally, support for None, Messages, Signals, Pools, Call Activity, Script Task, Message Flow, and Event Subprocess.

---

## Background

### Xklaim

Xklaim is a novel programming language that derives its core principles from the formal language KLAIM. 

---

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/khalidbourr/B2XKlaim
   cd B2XKlaim/B2XKlaim-FrontEnd
   npm install
   npm install jszip

   ```
## Usage

To utilize `B2XKlaim` effectively, follow these steps:

### 1. Start the Backend

```bash
   cd B2XKlaim/B2XKlaim-BackEnd
   mvn clean install (mvn clean install -DskipTests)
   mvn clean compile
```
Ensure the backend is up and running. Initiate the B2XKlaimApplication.

### 2. Launch the tool

Navigate to the root directory:

   ```bash
   cd B2XKlaim
   npm start
