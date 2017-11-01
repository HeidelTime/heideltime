@echo off
REM setting the environment for the HeidelTime kit
set CLASSPATH=%HEIDELTIME_HOME%\class;%CLASSPATH%
set CLASSPATH=%HEIDELTIME_HOME%\lib;%CLASSPATH%
REM setting the environment for DKPRO (required by HeidelTime)
set CLASSPATH=%DKPRO_HOME%\bin;%CLASSPATH%
set CLASSPATH=%DKPRO_HOME%\lib;%CLASSPATH%
REM setting the environment for jvntextpro (required for the wrapper)
set CLASSPATH=%JVNTEXTPRO_HOME%;%CLASSPATH%
REM setting the environment for the stanford pos tagger (required for the wrapper)
set CLASSPATH=%STANFORDTAGGER%;%CLASSPATH%
