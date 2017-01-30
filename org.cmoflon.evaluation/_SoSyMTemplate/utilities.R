getFormattedTime <- function() {
	format(Sys.time(), "%Y-%m-%d|%X")
}

#
#
# Example: log("Hi %s, today is the %s", "Jack", date())
#
log <- function(formatString, ...)
{
	newFormatString = sprintf("%s - %s", getFormattedTime(), formatString)
	print(sprintf(newFormatString, ...))
}

checkGlobalParentDir <- function()
{
	if (!dir.exists(parentDir))
		throw(sprintf("Parent dir %s does not exist. Stop.", parentDir))
}


closeAllDevices <- function() {
	while (dev.cur() != 1) {
		dev.off()
	}
}

cleanOuptutDirectory <- function(outputDirectory) {
	sapply(list.files(outputDirectory, patter="*.pdf$"), unlink)
	sapply(list.files(outputDirectory, patter="*.txt$"), unlink)
	sapply(list.files(outputDirectory, patter="*.csv$"), unlink)
}

