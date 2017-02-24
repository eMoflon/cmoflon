mkdir ./tars
for((i=35571;i<=35630;i++))
do
curl --user d.giessing:ortxfmy1 https://www.flocklab.ethz.ch/user/webdav/$i/results.tar.gz --output ./tars/$i.tar.gz
done
