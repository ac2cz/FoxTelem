# Generate a C header files needed for spacecraft from the FoxTelem payload file
# g0kla@arrl.net

import sys


#
# Main
#
if len(sys.argv) < 2:
    print ('Usage: gen_header <fileName.csv>')
    print ('Generate a spacecraft c header file from the FoxTelem payload file')
    sys.exit(1)
	
fileName = sys.argv[1]
line = ""
fields = []

# open the infile and read all the content in as a set of lines
try:
    with open(fileName) as infile:
        for line in infile:
            fields = line.split(',')
            print(fields[2])

except UnicodeDecodeError as e:
    print ("ERROR: Binary data found in the file.  Is it a CSV file?  Or the raw XLS?")
    print (e)
    print (line)
    print (fields)
    exit(1)