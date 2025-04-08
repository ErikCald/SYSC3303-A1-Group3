# SYSC3303-A1-Group3
This repository contains the code and documentation for the SYSC3303A1 Group 3 project.

## Project Breakdown
- The simulation codebase is located under `src/main/java`
- All respective tests are under `src/test/java`
- Input files for the simulation are located under `src/main/resources`. Default files are provided, but can be replaced with the same filenames. The following files are required:
  - `zone_location.csv`: Records of zones and their rectangular coordinates
  - `incident_file.csv`: Records of fire events, when they should occur, and additional metadata
  - `drone_faults.csv`: Records of faults that should occur for specific drones at specific times. If no faults are desired, remove the records rather than deleting the file.

## Compilation
This project uses Gradle for dependency management and compilation. The Gradle Wrapper is included in the project—you should not be required to install anything Gradle related to your machine.

1. Ensure JDK 21 is installed and configured as your JAVA_HOME Environment Variable. The [Eclipse Temurin](https://adoptium.net/temurin/) JDK binary by Adoptium was used by this group, but any JDK 21 should suffice.
2. Ensure your IDE, preferably IntelliJ IDEA, is fully updated (otherwise may not support JDK 21)
3. Clone the repo to your IDE
4. Execute `./gradlew clean build` using Git Bash if on Windows, or Bash if using a Unix-based OS

## Testing 
- Execute `./gradlew clean test` (as instructed above) to run all tests. 
- Alternatively, right-click on the `test` module using IntelliJ IDEA and select the "Run Tests" option.

## Running
- Use the Run functionality in IntelliJ IDEA on the [Main class](src/main/java/sysc3303/a1/group3/Main.java) to run the simulation.
- Alternatively, execute `./gradlew clean run`

Observe the graphical display popup, and the logging to the system output. The log can also later be viewed in output.txt (generated in the project roo).
The application will only terminate when the graphical display is closed.

## Responsibilities Breakdown (Iteration 4)
NOTE: The number of bullets points do NOT equally constitute the work done from each person.
Overall, we commited even effort and work to this iteration.

Abdul Aziz, Muhammad Maahir:
- UML

Caldwell, Erik:
- State Machine for Errors
- Drone Stuck Error Events Handling

Chelliah, Lakshman
- Drone Error Event Distribution and Timing

Hassan, Ayoub:
- Drone Error Event for Nozzle Stuck

Labonté-Hagar, Zane
- Integration work 
- Bug fixes for end code

Xia, Tim
- Integration work (specifically for Nozzle Stuck)
- Corrupted Messages Drone Error Code
