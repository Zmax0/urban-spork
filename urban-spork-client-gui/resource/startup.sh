#!/bin/bash
cd "$(dirname "$0")"
./runtime/bin/java -Xms64m -Xmx256m -jar urban-spork-client-gui.jar
