#!/bin/bash

rm -r logs

PID=$(lsof -ti:1099)

if [ ! -z "$PID" ]; then
    echo "Port 1099 is already in use. Killing existing process..."
    kill -9 $PID
    echo "Existing RMI Registry process killed."
fi

# Start RMI Registry
rmiregistry 1099 &
echo "RMI Registry started"
sleep 2  # Ensures the registry has time to start

# Start Node0
java -cp . Main 0 1099 &
sleep 5  # Wait for Node0 to initialize

# Start subsequent nodes
for i in {1..7}
do
  java -cp . Main $i 1099&
  echo "Node$i is starting and joining the ring..."
  sleep 2  # Ensures each node has time to initialize and join
done

echo "All nodes have been initialized."
