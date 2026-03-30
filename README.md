# B2XKlaim

<img src="https://github.com/PROSLab/B2XKlaim/blob/main/b2xklaim.jpg" width="500" height="auto">

Translates BPMN Collaboration diagrams into [Xklaim](https://github.com/LorenzoBettwordsini/Xklaim) skeleton code.

![B2XKlaim Screenshot](https://github.com/khalidbourr/B2XKlaim/blob/main/Screenshot%20from%202025-02-26%2022-10-16.png)

## Supported BPMN Elements

- Gateways: XOR, AND
- Events: Start, Intermediate, End (None, Message, Signal)
- Tasks: Script Task, Call Activity
- Pools, Message Flow, Event Subprocess

## Setup

### Backend (Java 21 + Spring Boot)

```bash
cd B2XKlaim/B2XKlaim-BackEnd
mvn clean install -DskipTests
mvn clean compile
```

Then run `B2XKlaimApplication`.

### Frontend (Vue 3 + bpmn-js)

```bash
cd B2XKlaim/B2XKlaim-FrontEnd
npm install
```

### Run

```bash
cd B2XKlaim
npm start
```

Draw a BPMN Collaboration diagram in the editor, hit translate, and download the generated Xklaim code.

## How to Use

See the full guide: [How to use B2XKlaim](https://kbourr.com/how-to-use-b2xkaim-tool/)

A hosted version is available at [kbourr.com/bxklaim](https://kbourr.com/bxklaim) — a new release will be deployed soon.