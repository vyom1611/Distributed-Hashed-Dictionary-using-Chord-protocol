# Distributed-Hashed-Dictionary-using-Chord-protocol

Chord Distributed Dictionary README

Vyom Sharma - sharm843

## Instructions

### Running the Program

`javac *.java`

`./start_chord`

in seperate terminal:

`java -cp . Client Node0`

## Status Disclosure

### Code Status

Compilation: The code compiles without errors.

Execution: The program runs without crashing.

Node Join: Node join functionality.

Insert and Lookup: Both insert and lookup functionalities are working correctly.

Dictionary Loading: Dictionary loading functionality is implemented.

### Known Bugs
Finger table generation is bugged, every node loads the entire dictionary.

## Log File

The log file generated are in the folder 'logs' by running the system are named node-{id}-log.log
