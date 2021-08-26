
<!-- PROJECT LOGO -->

<p align="center"> </p>
    <h2 align="center">Surgery Schedule Suggestion System </h2>
    <h3 align="center"> Assuta Hospital</h3>




<!-- TABLE OF CONTENTS -->
<details open="open">
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
        <li><a href="#preparing">Preparing</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgements">Acknowledgements</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## About The Project
<!-- TODO -->
[![Scheduling Screen Example][scheduling-screenshot]](https://imgur.com/xsrniTG)  
[![Statistics Screen Example][statistics-screenshot]](https://imgur.com/GFrkHmB)

Assuta Hospital have a problem.  
When it's come to schedule surgeries blocks, usually they have constants doctors that coming in constant time - more or less - and filling those blocks.  
But sometimes they have a gap in the schedule, and they need to improvise.  
This improvisation leads in many cases to resting or hospitalize beds shortage, because they don't know to predict when each one of them will release.

This project main propose is to help them fill those gaps, as smart and profitable as can.

The second propose, which raised by the hospital manager in the middle of the project, is to help him - the manager - to see the performances of his doctors and the differences between them.  

### Built With

These are the main libraries I used in the project.

* [scalafx](https://www.scalafx.org/)
* [controlsfx](https://controlsfx.github.io/)
* [apache](http://www.apache.org/)
* [hsqldb](http://hsqldb.org/)
* [scala-slick](https://scala-slick.org/)
* [akka](https://akka.io/)
* [joda-time](https://www.joda.org/joda-time/)



<!-- GETTING STARTED -->
## Getting Started

Here you can find explanation about how to install and prepare the program for use.

### Prerequisites

For running this project on your computer, you need to have:
* [Java](https://www.java.com/en/download/manual.jsp) 1.8 or later

### Installation


1. Download and extract the [installer]() <!-- TODO real link -->.

2. Open 'installer.exe' and install.

### Preparing
Since Assuta can't let any sensitive information to get out of their secure system, we forced to load all the data locally, from Excel files providing by the user.

In the **"file"** menu you can find under **"Load Configurations"** the items
* **"Past Surgeries"**
* **"Profit"**
* **"Doctors ID Mapping"**
* **"Surgery ID Mapping"**

as well as **"Load Schedule"** which appears only in the schedule part.

Those will let you load the relevant files, so the program can operate.


### Files Format 
Notes 
- First column ("A") index is 0.  
- Date format is "dd/MM/yyyy HH:mm" (e.g. "26/08/2021 15:00").

**Past Surgeries:** 

| Value          | Column | Type   |
|----------------|--------|--------|
| Doctor ID      | 2      | Int    |
| Operation Code | 3      | Double |
| Surgery Start  | 20     | Date   |
| Surgery End    | 23     | Date   |
| Resting Start  | 25     | Date   |
| Resting End    | 26     | Date   |
| Block Start    | 27     | Date   |
| Block End      | 28     | Date   |
| Release Date   | 29     | Date   |

**Profit:**

| Value          | Sheet  | Column | Type   |
|----------------|--------|--------|--------|
| Doctor ID      | First  | 0      | Int    |
| Profit         | First  | 1      | Int    |
| Operation Code | Second | 0      | Double |
| Profit         | Second | 1      | Int    |

**Doctors ID Mapping:**


| Value          | Column | Type   |
|----------------|--------|--------|
| Doctor ID      | 0      | Int    |
| Doctor Name    | 1      | String |

**Surgery ID Mapping:**

| Value          | Column | Type   |
|----------------|--------|--------|
| Operation Code | 0      | Double |
| Operation Name | 1      | String |

 **Schedule:**

| Value                 | Column | Type   |
|-----------------------|--------|--------|
| Doctor ID             | 2      | Int    |
| Operation Code        | 3      | Double |
| Surgery Planned Start | 10     | Date   |
| Operation Room        | 13     | Int    |
| Block Start           | 27     | Date   |
| Block End             | 28     | Date   |
| Release Date          | 29     | Date   |

<!-- USAGE EXAMPLES -->
## Usage

There are two launchers 
1. **Scheduling** - The main one:  
   In the screen you will see the daily schedule blocks.  
   When pressing the **Get suggestions** button at the top, you can choose the time window for the suggestions,  
   Then you will get the top options for each doctor, ordered by weighted profitability.  
   You can configure the weights in the setting menu.
   

2. **Statistics**  
   Here you can see a list of all the doctors working in Assuta (if you loaded the right data).
   There are 3 modes here, and you can switch between them from the menu:
   * **Basic Information:**  
     Shows information about each doctor by himself  
   * **Improvement Information - Average:**  
     Shows average information about each doctor VS the average
   * **Improvement Information By Operation:**  
      Same, but here you need to choose one operation, and you'll see only the doctors performing that operation, compared to the data about that one.


<!-- LICENSE -->
## License

Distributed under the MIT License. See `LICENSE` for more information.



<!-- CONTACT -->
## Contact

Nati Albert - [Linkedin](https://www.linkedin.com/in/netanel-albert-9b0119159/) - NetanelAlbert@gmail.com

Project Link: [https://github.com/NetanelAlbert/Ariel-Final-Project---Asuta-Scheduling.git](https://github.com/NetanelAlbert/Ariel-Final-Project---Asuta-Scheduling.git)


<!-- ACKNOWLEDGEMENTS -->
## Acknowledgements
* [GitHub Readme template](https://github.com/othneildrew/Best-README-Template)




[scheduling-screenshot]: src/main/resources/screenshotes/scheduling.png
[statistics-screenshot]: src/main/resources/screenshotes/statistics.png

