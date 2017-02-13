EXPERIMENTS=(kTCMan lkTCMan lmstMan kTCGen lkTCGen lmstGen)
runs=$1
start=$2
echo "Retrieving Data from startId $2 for $1 runs."
for((i=0;i<$runs;i++))
	do
	for a in "${EXPERIMENTS[@]}"
	do
		offset=0;
		case $a in
			kTCMan) offset=0;;
			lkTCMan) offset=1;;
			lmstMan) offset=2;;
			kTCGen) offset=3;;
			lkTCGen) offset=4;;
			lmstGen) offset=5;;
		esac 	
		id=$(($start+ 6*$i+$offset))
		mkdir ./$a/$id
		echo "Retrieving $a Experiment with id $id"
		curl --user d.giessing:ortxfmy1 https://www.flocklab.ethz.ch/user/webdav/$id/results.tar.gz --output ./$a/$id/results.tar.gz
		tar -xzf ./$a/$id/results.tar.gz -C ./$a
		rm ./$a/$id/results.tar.gz
	done
done
