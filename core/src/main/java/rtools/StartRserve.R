if(is.element('Rserve', installed.packages()[,1]) == FALSE)
{
	install.packages('Rserve');
}
library('Rserve');
Rserve(args="--no-save");
