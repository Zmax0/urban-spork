@echo off
set server="urban-spork-client-gui"
start javaw --add-opens java.base/java.lang.reflect=ALL-UNNAMED -jar %server%.jar
exit