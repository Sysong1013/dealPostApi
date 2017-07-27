@echo off & setlocal enabledelayedexpansion

set LIB_JARS=""
cd ..\lib
for %%i in (*) do set LIB_JARS=!LIB_JARS!;..\lib\%%i
cd ..\bin

set MAIN_CLASS=com.dangdang.stock.dealpost.StockDealPostServiceProviderMain

if ""%1"" == ""debug"" goto debug
if ""%1"" == ""jmx"" goto jmx

java -Xms1024m -Xmx1024m -XX:MaxPermSize=64M -classpath ..\conf;%LIB_JARS% %MAIN_CLASS%
goto end

:end
pause