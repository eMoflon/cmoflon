#!/bin/bash

outputFile="CodeSizeRawData.csv"
header="ImageFile,Algorithm,TextInByte,DataInByte,BssInByte,TotalInByte"
csvSep=","

echo $header

for f in kTCMan kTCGen lkTCMan lkTCGen lmstMan lmstGen NoTC;
do

  outputOfSize=$(size ../$f/$f.sky | tail -1)
  imageFile=$(basename $(echo $outputOfSize | cut -d" " -f6))
  textSize=$(echo $outputOfSize | cut -d" " -f1)
  dataSize=$(echo $outputOfSize | cut -d" " -f2)
  bssSize=$(echo $outputOfSize | cut -d" " -f3)
  totalSize=$(echo $outputOfSize | cut -d" " -f4)
  [ "$imageFile" == "kTCGen.sky" ] && algorithm="kTC-gen"
  [ "$imageFile" == "kTCMan.sky" ] && algorithm="kTC-man"
  [ "$imageFile" == "lkTCGen.sky" ] && algorithm="l*-kTC-gen"
  [ "$imageFile" == "lkTCMan.sky" ] && algorithm="l*-kTC-man"
  [ "$imageFile" == "lmstMan.sky" ] && algorithm="LMST-man"
  [ "$imageFile" == "lmstGen.sky" ] && algorithm="LMST-gen"
  [ "$imageFile" == "NoTC.sky" ] && algorithm="NoTC"
  echo "$imageFile$csvSep$algorithm$csvSep$textSize$csvSep$dataSize$csvSep$bssSize$csvSep$totalSize"
done
