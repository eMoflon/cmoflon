start=$1
end=$2
for((i=$start;i<=$end;i++))
do
./flocklab -d $i
done
