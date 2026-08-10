# B2XKlaim

[![Build](https://github.com/khalidbourr/B2XKlaim/actions/workflows/build.yml/badge.svg)](https://github.com/khalidbourr/B2XKlaim/actions/workflows/build.yml)

<p align="center">
  <img src="https://github.com/PROSLab/B2XKlaim/blob/main/b2xklaim.jpg" width="500" height="auto">
</p>

**B2XKlaim** combines two complementary roles. First, it provides an **intuitive visual modeling language** based on BPMN that lets multidisciplinary teams (roboticists, software engineers, and non-technical stakeholders) specify multi-robot missions with minimal learning curve, using a notation readable at a glance. Second, it acts as a **code generator** that transforms these diagrams into executable [X-Klaim](https://github.com/LorenzoBettini/Xklaim) skeletons, abstracting the low-level tuple-space coordination so developers focus on task logic rather than synchronization plumbing. Together, these capabilities close the gap between high-level mission design and executable robot coordination.

## How It Works

B2XKlaim follows a **model-driven development** pipeline:

1. **Design.** Model your multi-robot mission as a BPMN Collaboration diagram using the built-in web editor (powered by [bpmn-js](https://github.com/bpmn-io/bpmn-js)). Each pool represents a robot or participant, and message flows capture inter-robot communication via tuple spaces.

2. **Parse.** The backend parses the `.bpmn` XML, extracting pools, tasks, gateways, events, and message flows into an intermediate representation.

3. **Translate.** A set of mapping rules transforms each BPMN element into its X-Klaim counterpart: pools become `net` declarations, message flows map to `out`/`in` operations on tuple spaces, XOR gateways translate to conditionals, AND gateways to parallel constructs, EB gateways to timeout-based routing (`if ... within`), LP gateways to `while` loops, and tasks become `proc` bodies. See [docs/translation.md](docs/translation.md) for the full rule set.

4. **Generate.** The tool outputs a structured `.xklaim` package:

```
src/main/java/xklaim/
├── Collaboration.xklaim        # net definition
├── processes/                   # one proc per pool
├── tasks/                       # script task stubs
└── branches/                    # AND gateway branches
```

The generated code is downloadable as a ready-to-compile project for the X-Klaim runtime with ROS 2 integration.

<p align="center">
  <img src="https://github.com/khalidbourr/B2XKlaim/blob/development/media/rover_drone_example.png" width="90%">
  <br><em>BPMN Collaboration diagram designed in the B2XKlaim editor</em>
</p>

<p align="center">
  <img src="https://github.com/khalidbourr/B2XKlaim/blob/development/media/generated_rover_drone.png" width="90%">
  <br><em>Generated X-Klaim code output</em>
</p>


### X-Klaim

To compile and run the generated code, you need [X-Klaim](https://github.com/LorenzoBettini/xklaim):

- **Eclipse update site**: https://lorenzobettini.github.io/xklaim-releases/

- **Eclipse distributions with X-Klaim pre-installed**: https://sourceforge.net/projects/xklaim/files/products/


## Supported BPMN Elements

| Category | Elements |
|----------|----------|
| **Gateways** | XOR (Exclusive), AND (Parallel), EB (Event-Based), LP (Loop) |
| **Events** | Start, Intermediate, End (None, Message, Signal, Timer variants) |
| **Tasks** | Script Task, Call Activity |
| **Structure** | Pools, Message Flow, Event Subprocess |

## Setup

### Xklaim

Xklaim is a novel programming language that derives its core principles from the formal language KLAIM. 

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

Draw a BPMN Collaboration diagram in the editor, hit **Translate**, and download the generated X-Klaim code.

## How to Use

See the full guide: [How to use B2XKlaim](https://kbourr.com/how-to-use-b2xkaim-tool/)

A hosted version is available at [kbourr.com/bxklaim](https://kbourr.com/bxklaim); a new release will be deployed soon.

## References

If you use B2XKlaim in your research, please cite:

> K. Bourr, F. Tiezzi, L. Bettini, and S. Seriani, "Low-Code Data-Aware Programming for Multi-Robot Missions: From BPMN to X-Klaim and ROS," in *International Symposium on Leveraging Applications of Formal Methods (ISoLA)*, pp. 224–242, 2024, Springer.

> K. Bourr, F. Tiezzi, L. Bettini, and S. Seriani, "Translating BPMN models into X-KLAIM programs for developing multi-robot missions," *International Journal on Software Tools for Technology Transfer*, pp. 1–19, 2026, Springer.

> K. Bourr, F. Tiezzi, and L. Bettini, "Model-driven development of multi-robot systems: from BPMN models to X-Klaim code," in *International Symposium on Leveraging Applications of Formal Methods (ISoLA)*, pp. 224–242, 2024, Springer.

<details>
<summary>BibTeX</summary>

```bibtex
@article{bourr2026translating,
  title     = {Translating BPMN models into X-KLAIM programs for developing multi-robot missions},
  author    = {Bourr, Khalid and Tiezzi, Francesco and Bettini, Lorenzo and Seriani, Stefano},
  journal   = {International Journal on Software Tools for Technology Transfer},
  pages     = {1--19},
  year      = {2026},
  publisher = {Springer}
}

@inproceedings{bourr2024model,
  title        = {Model-driven development of multi-robot systems: from BPMN models to X-Klaim code},
  author       = {Bourr, Khalid and Tiezzi, Francesco and Bettini, Lorenzo},
  booktitle    = {International Symposium on Leveraging Applications of Formal Methods},
  pages        = {224--242},
  year         = {2024},
  organization = {Springer}
}
```

</details>

## License

See [LICENSE](LICENSE) for details.
