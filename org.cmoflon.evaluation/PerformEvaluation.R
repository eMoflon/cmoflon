# 
# Author: Roland Kluge
# Created: 2017-01-30
###############################################################################

options(error=traceback)
require(xtable)

cMoflonBlue = rgb(0, 162, 232, max=255)
GRID_COL = "lightgray"
GRID_LINE_TYPE = 6

COL_GEN_VS_MAN = "Gen. / man. [\\%]" # w.r.t. text
COL_A_VS_NOTC = "Algo. / NoTC [\\%]" # w.r.t. text
COL_FILE = "File"
COL_ALGORITHM = "Algorithm A"
COL_DATA = "Data [B]"
COL_TEXT= "Text [B]"
COL_BSS = "BSS [B]"
COL_TOTAL = "Total [B]"

# Derived columns
COL_SIZE = "\\MetricCodeMemorySize{A}"
COL_SIZE_DIFF_TO_NOTC = "$\\Delta_{A-NoTC}\\MetricCodeMemorySize$"
COL_SIZE_RELDIFF_TO_NOTC = "$\\Delta_{A-NoTC}\\MetricCodeMemorySize$[\\%]"
COL_SIZE_DIFF_GEN_TO_MAN = "$\\Delta_{Gen-Man}\\MetricCodeMemorySize$"
COL_SIZE_RELDIFF_GEN_TO_MAN = "$\\Delta_{Gen-Man}\\MetricCodeMemorySize$[\\%]"


source("./_SoSyMTemplate/utilities.R")

parentDir="."

main <- function()
{
	outputDir = sprintf("%s/output", parentDir)
	checkGlobalParentDir()
	cleanOuptutDirectory(outputDir)
	if (!dir.exists(outputDir))
		dir.create(outputDir)
	
## Read in
	log("Start processing.")
	codeSizeData = read.csv("input/CodeSizeRawData.csv")
	runtimeData = read.csv("input/RuntimeRawData.csv")
	log("Read input data: %d lines for code size, %d lines for runtime.", nrow(codeSizeData), nrow(runtimeData))
	
## Process code size data
	names(codeSizeData) = c(COL_FILE, COL_ALGORITHM, COL_TEXT, COL_DATA, COL_BSS, COL_TOTAL)	
	
	
	rowOfNoTC = grep("NoTC", codeSizeData[[COL_ALGORITHM]])
	
	if (length(rowOfNoTC) == 0)
		stop("Missing line for NoTC")
		
	# Calculate Ms(A)
	codeSizeData[[COL_SIZE]] = codeSizeData[[COL_DATA]] + codeSizeData[[COL_TEXT]]
	
	# Calculate abs. increase compared to NoTC
	sizeOfNoTC = codeSizeData[[rowOfNoTC, COL_SIZE]]
	codeSizeData[[COL_SIZE_DIFF_TO_NOTC]] = codeSizeData[[COL_SIZE]] - sizeOfNoTC
	codeSizeData[[COL_SIZE_RELDIFF_TO_NOTC]] = codeSizeData[[COL_SIZE_DIFF_TO_NOTC]] / sizeOfNoTC * 100.0
	

	# Compare gen vs. man for each algorithm
	codeSizeData[[COL_SIZE_DIFF_GEN_TO_MAN]] = NA
	codeSizeData[[COL_SIZE_RELDIFF_GEN_TO_MAN]] = NA
	for (genIndex in grep("-gen", codeSizeData[[COL_ALGORITHM]]))
	{
		manIndex = genIndex - 1
		codeSizeData[genIndex,][[COL_SIZE_DIFF_GEN_TO_MAN]] = codeSizeData[genIndex, ][[COL_SIZE]] - codeSizeData[manIndex, ][[COL_SIZE]]
		codeSizeData[genIndex,][[COL_SIZE_RELDIFF_GEN_TO_MAN]] = codeSizeData[genIndex,][[COL_SIZE_RELDIFF_GEN_TO_MAN]] / codeSizeData[manIndex, ][[COL_SIZE]] * 100.0
	}
		
	# Compare code size with NoTC
	
	textSizeOfNoTC = codeSizeData[rowOfNoTC,][[COL_TEXT]]
	codeSizeData[[COL_A_VS_NOTC]] = codeSizeData[[COL_TEXT]] / textSizeOfNoTC * 100.0
	#codeSizeData[rowOfNoTC,][[COL_A_VS_NOTC]] = NA
	
	print("--- RQ1: Raw data ---")
	print(codeSizeData)
	
	printedDataHeader = c(COL_ALGORITHM, COL_SIZE, COL_SIZE_DIFF_TO_NOTC, COL_A_VS_NOTC)
	intermediateLinePositions = c(2, 4, 6) # stores the pos. of the \toprule,\midrule,\bottomrule lines
	digitsSpec = c(0, 0, 0, 1, 1)
	printedData = codeSizeData[,printedDataHeader]
	print("--- RQ1: LaTeX ---")
	print(printedData)
	formattedCodeSizeTable = xtable(
		printedData,
		caption = sprintf("Code size of the sensor images (\\RQCode)"),
		digits = digitsSpec,
		align=c("l", "r|", "r", "r", "r"),
		label = "tab:RQCode"
		)
	
	
	print(formattedCodeSizeTable,
			comment = FALSE,
			file = sprintf("%s/%s.tex", outputDir, "RQCodeSizeTable"),
			hline.after = c(-1, 0, intermediateLinePositions, nrow(printedData)),
			sanitize.colnames.function = myBold, 
			include.rownames=FALSE,
			table.placement = "hbtp",
			caption.placement = "top", 
			include.colnames = T,
			floating = T, 
			booktabs = T, 
			type="latex")	
	
## Process runtime data
	# Change spacing of ticks: https://stackoverflow.com/questions/3785089/r-change-the-spacing-of-tick-marks-on-the-axis-of-a-plot
	tryCatch({
	pdf(file="output/RuntimeBoxplot.pdf",
			width=10, height=6
			)
		
		par(mar=c(3.5, 4, 1, 1))
		boxplotColumnLabels = gsub("-man", "-m", gsub("-gen", "-g", runtimeData$Algorithm))
		boxplot(RuntimeInMillis~boxplotColumnLabels, data=runtimeData)
		#minor.tick(0,2,0.5) # library(Hmisc)
		grid(NULL, NULL, lty = GRID_LINE_TYPE, col = GRID_COL)
		par(new=TRUE)
		plot = boxplot(RuntimeInMillis~boxplotColumnLabels,
			data=runtimeData,
			col=c("gray", cMoflonBlue),
			#xlab="Algorithm",
			ylab="Runtime[ms]"
		)
		
		#print(summary(plot$stats))
	
	}, finally={closeAllDevices()});
		
	log("Processing complete.")
}

myBold <- function(x) {
	sanitizedColnames = sprintf("\\multicolumn{1}{c}{\\textbf{%s}}", x)
	sanitizedColnames[1] = sprintf("\\multicolumn{1}{c|}{\\textbf{%s}}", x[2])
	sanitizedColnames
}

main()