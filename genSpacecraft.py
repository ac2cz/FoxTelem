# Generate the spacecraft files needed for FoxTelem from the csv file that defines
# the Downlink Spec
# g0kla@arrl.net

import sys

lineNum = 0;

def processStructure(type, file):
    "This processes a structure section of the document"
    global lineNum #otherwise the assignment below automagically creates a local lineNum variable
    structure = ""
    columns = [2, 3, 7, 6, 13, 14, 15, 16, 1, 8]
    for line in infile:
        fields = line.split(',')
        if ('END' in fields[0]):
            return structure
        if ("//" not in fields[0]):
            structure += str(lineNum)
            lineNum = lineNum + 1
            structure += ','
            structure += type
            for col in columns:
                if (fields[col] == ''):
                    fields[col] = '-'
                if (fields[col] == 'battAv'):
                    fields[col] = 'BATT_A_V'
                if (fields[col] == 'battBv'):
                    fields[col] = 'BATT_B_V'
                if (fields[col] == 'battCv'):
                    fields[col] = 'BATT_C_V'
                structure += ','
                structure += fields[col].rstrip('\n')
            structure += '\n'

if len(sys.argv) <= 3:
    print ('Usage: genSpacecraft <FOX-ID> <RT|MAX|MIN|RAD|WOD> <fileName.csv>')
    print ('Generate the spacecraft files needed for FoxTelem from the csv file that defines the downlink specification')
    sys.exit(1)

foxId = sys.argv[1]    
type = sys.argv[2]
fileName = sys.argv[3]
outFileName = foxId + '_rttelemetry.csv'

if (type == "MAX"):
    outFileName = foxId + '_maxtelemetry.csv'
if (type == "MIN"):
    outFileName = foxId + '_mintelemetry.csv'
if (type == "RAD"):
    outFileName = foxId + '_radtelemetry.csv'
if (type == "WOD"):
    outFileName = foxId + '_wodtelemetry.csv'
    
print ('Processing '+ type + ' from file: ' + fileName)

# open the infile and read all the content in as a set of lines
with open(fileName) as infile:
    for line in infile:
        fields = line.split(',')
    	# make sure this is not a comment row
        if ("//" not in fields[0]):
            
            if ("Structure:" in fields[0]):
                if ("header" not in fields[1]):
                    if ("common" in fields[1]):
                        print("COMMON:" + fields[0] + " " + fields[1])
                        commonStructure = processStructure(type, infile)
                    if (type.lower() in fields[1]):
                        print("TYPE: " + fields[0] + " " + fields[1])
                        typeStructure = processStructure(type, infile)

# Open the output file and write out the header and the structures
outfile = open(outFileName, "w" )
outfile.write(str(lineNum) + "," + 
    "TYPE" + "," + 
    "FIELD" +"," +
    "BITS" +"," +
    "UNIT" +"," +
    "CONVERSION" +"," +
    "MODULE" +"," +
    "MODULE_NUM" +"," +
    "MODULE_LINE" +"," +
    "LINE_TYPE" +"," +
    "SHORT_NAME" +"," +    
    "DESCRIPTION" + '\n')
outfile.write(commonStructure)
outfile.write(typeStructure) 
outfile.close()
infile.close()