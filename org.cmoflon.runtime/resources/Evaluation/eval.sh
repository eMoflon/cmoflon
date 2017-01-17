pathToEval="/mnt/c/topology-control-evaluation/evaluation"
cd $pathToEval
pathToExperiment=$1
length=$(($2-1))
./cli.php --testbed flocklab --evaluation graph-neighborhood --source $pathToExperiment/serial.csv --destination $pathToExperiment/original.png --graph-minute 2
./cli.php --testbed flocklab --evaluation graph-neighborhood --source $pathToExperiment/serial.csv --destination $pathToExperiment/end.png --graph-minute $length
./cli.php --testbed flocklab --evaluation energy-milliamperehour --source $pathToExperiment/serial.csv --destination $pathToExperiment/energy.csv
sed -i -e 's/,/;/g' $pathToExperiment/energy.csv
sed -i -e 's/\./,/g' $pathToExperiment/energy.csv
