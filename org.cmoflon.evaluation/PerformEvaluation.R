# 
# Author: Roland Kluge
# Created: 2017-01-30
###############################################################################

options(error=traceback)
library(Hmisc)

cMoflonBlue = rgb(0, 162, 232, max=255)
GRID_COL = "lightgray"
GRID_LINE_TYPE = 6

source("./_SoSyMTemplate/utilities.R")

parentDir="."

main <- function()
{
	outputDir = sprintf("%s/output", parentDir)
	checkGlobalParentDir()
	cleanOuptutDirectory(outputDir)
	dir.create(outputDir)
	
## Read in
	log("Start processing.")
	codeSizeData = read.csv("input/CodeSizeRawData.csv")
	runtimeData = read.csv("input/RuntimeRawData.csv")
	log("Read input data: %d lines for code size, %d lines for runtime.", nrow(codeSizeData), nrow(runtimeData))

## Process code size data
	
## Process runtime data
	# Change spacing of ticks: https://stackoverflow.com/questions/3785089/r-change-the-spacing-of-tick-marks-on-the-axis-of-a-plot
	tryCatch({
	pdf(file="output/RuntimeBoxplot.pdf",
			width=10, height=6
			)
		
		par(mar=c(3.5, 4, 1, 1))
		boxplotColumnLabels = gsub("-man", "-m", gsub("-gen", "-g", runtimeData$Algorithm))
		boxplot(RuntimeInMillis~boxplotColumnLabels, data=runtimeData)
		#minor.tick(0,2,0.5)
		grid(NULL, NULL, lty = GRID_LINE_TYPE, col = GRID_COL)
		par(new=TRUE)
		plot = boxplot(RuntimeInMillis~boxplotColumnLabels,
			data=runtimeData,
			col=c("gray", cMoflonBlue),
			#xlab="Algorithm",
			ylab="Runtime[ms]"
	)
		
	print(summary(plot$stats))
	
	}, finally={closeAllDevices()});
	
	#print(codeSizeData)
	# print(runtimeData)
	
	log("Start done.")
}
main()