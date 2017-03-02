numRuns=$1
echo "Submitting $numRuns testRuns"
for((i=0;i<$numRuns;i++))
do
	echo "Round $(($i+1))"
	for((j=1;j<7;j++))
	do
		echo "Submitting test$j.xml"
		./flocklab -v test$j.xml
		./flocklab -c test$j.xml
	done
done
