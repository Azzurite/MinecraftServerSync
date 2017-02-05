@ECHO OFF

start "" javaw -jar "%~dp0\lib\${project.build.finalName}.jar"
