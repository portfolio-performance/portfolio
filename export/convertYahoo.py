# -*- coding: utf-8 -*-
import os
import csv

# Convert Portfolio-performance csv-export files to yahoo finance csv format.
# Purpose: Portfolio performance is great but not mobile. The Yahoo finance export enable portfolio overview for mobile devices via Yahoo.
# License: Eclipse Public License - v 1.0
# Author: Dirk Balthasar, 2019
#
# Prerequisite:
# 1. Create free yahoo finance account. (This is not a recommendation from my side for this product) 
# 2. Be aware that your data will be uploaded into the cloud
# 3. Make sure that you are fine with the yahoo terms of usage
#
# Usage: 
# 0. Install python 3
# 1. Export portfolio: File->Export
# 2. Copy scrip convert.py to export directory
# 3. run convert.py (script opens all csv files and creates one csv file "yahoo.csv" that can be uploaded to yahoo finance
# 4. Import yahoo.csv to yahoo finance account
# Portfolio should occur in "my portfolios/Imported from Yahoo"
#
# Restrictions: Format relies on the csv format of PP and Yahoo. Whenever the csv format is altered, this script will fail and needs to be adapted. 

# parse csv file and reorder columns from PP to Yahoo order
# 0     1   2    3                4            5                     6           7         8       9      10   11  12            13             14
# Datum,Typ,Wert,Buchungswaehrung,Bruttobetrag,Waehrung Bruttobetrag,Wechselkurs,Gebuehren,Steuern,Stueck,ISIN,WKN,Ticker-Symbol,Wertpapiername,Notiz
# ->
#0      1             2    3    4       5   6    7   8      9          10             11       12         13         14        15
#Symbol,Current Price,Date,Time,Change,Open,High,Low,Volume,Trade Date,Purchase Price,Quantity,Commission,High Limit,Low Limit,Comment
# all index zero based counting
def parseCSV(filename):
	filenamePlain = os.path.splitext(os.path.basename(filename))[0]
	portfolio = []
	filter = [0,2, 9, 12]
	target = [9,10,11,0]
	with open(filename) as csvfile:
		readerCSV = csv.reader(csvfile	, delimiter=';', quotechar='|')
		line_count = 0
		for r in readerCSV:
			if (line_count == 0):
				if (r != ['Datum','Typ','Wert','Buchungswährung','Bruttobetrag','Währung Bruttobetrag','Wechselkurs','Gebühren','Steuern','Stück','ISIN','WKN','Ticker-Symbol','Wertpapiername','Notiz']):
					return portfolio
				else:
					print('Parsing' + filename) 
			line_count += 1
			i = 0
			row = ['', '', '', '', '', '', '','','','','','','','','','']
			for cell in r:
				if i in filter:
					row[target[filter.index(i)]]=cell
				i+=1
			if (line_count>1):
				portfolio.append(row)
	return portfolio

def isnumeric(str):
	try:
		float(str)
		return True
	except ValueError:
		return False

# locale 1.000,23 -> 1000.23
def repairNumber(str):
	str = str.replace(".", "", 1)
	str = str.replace(",", ".", 1)
	return str
	
# locale  2017-07-29Tx -> 20190729
def repairDate(str):
	str = str.split("T")[0]
	str = str.replace("-","",2)
	return str

# write portfolio to csv
def writeCSV(portfolio, writerCSV):
	for entry in portfolio:
		key = 0
		entry[9] = repairDate(entry[9])
		entry[10] = repairNumber(entry[10])
		entry[11] = repairNumber(entry[11])
		for value in entry:
			if (key == 10 and isnumeric(value) and isnumeric(entry[11])):
					val = float(value)
					pieces = float(entry[11])
					if (val < 0):
						value = -val / pieces
						entry[11] = -pieces
					else:					
						value = val / pieces
			entry[key] = value
			key += 1
		writerCSV.writerow(entry)

def scanCsvAndEportYahoo():
	portfolios = []
	for file in os.listdir("."):
		if file.endswith(".csv"):
			portfolios.append(parseCSV(file))
	YahooFile = 'yahoo.csv'
	with open(YahooFile, mode='wb') as fileCsv:
		writerCSV = csv.writer(fileCsv, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
		writerCSV.writerow(['Symbol','Current Price','Date','Time','Change','Open','High','Low','Volume','Trade Date','Purchase Price','Quantity','Commission','High Limit','Low Limit','Comment'])
		for portfolio in portfolios:
			writeCSV(portfolio, writerCSV)
	print('wrote: ' + YahooFile)

scanCsvAndEportYahoo()
