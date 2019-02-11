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
    for line in file:
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
                if (col == 13):
                    if (type == 'maxDownlink' or type == 'minDownlink'):
                        # If this shows all values no need for MAX/MIN to have the row
                        if ('3' in fields[16] or '4' in fields[16]):
                            fields[13] = 'NONE'
                structure += fields[col].rstrip('\n')
            structure += '\n'

if len(sys.argv) <= 3:
    print ('Usage: genSpacecraft <FOXID> <rt|max|min|rad|wod|CAN|CANWOD> <fileName.csv>')
    print ('Generate the spacecraft files needed for FoxTelem from the csv file that defines the downlink specification')
    sys.exit(1)

foxId = sys.argv[1]    
type = sys.argv[2]
fileName = sys.argv[3]
outFileName = foxId + '_rttelemetry.csv'
commonStructure = "";

if (type.lower() == "max"):
    outFileName = foxId + '_maxtelemetry.csv'
    type = "maxDownlink"    
if (type.lower() == "min"):
    outFileName = foxId + '_mintelemetry.csv'
    type = "minDownlink"    
if (type.lower() == "rad"):
    outFileName = foxId + '_radtelemetry.csv'
if (type.lower() == "wod"):
    outFileName = foxId + '_wodtelemetry.csv'
    type = "wodSpecificDownlink"    
if (type.lower() == "can"):
    outFileName = foxId + '_exptelemetry.csv'
    type = "CANHealth"    
if (type.lower() == "canwod"):
    outFileName = foxId + '_wodexptelemetry.csv'
    type = "CANWOD"    
if (type.lower() == "rt"):
    type = "realtimeDownlink"    
print ('Processing '+ type + ' from file: ' + fileName)

line = ""
fields = []
# open the infile and read all the content in as a set of lines
try:
    with open(fileName) as infile:
        for line in infile:
            fields = line.split(',')
            # make sure this is not a comment row
            if ("//" not in fields[0]):            
                if ("Structure:" in fields[0]):
                    if ("header" not in fields[1]):
                        if (not type == "CANHealth" and not type == "CANWOD"):
                            if ("common" in fields[1]):
                                print("COMMON:" + fields[0] + " " + fields[1])
                                commonStructure = processStructure(type, infile)
                        if (type in fields[1]):
                            print("TYPE: " + fields[0] + " " + fields[1])
                            typeStructure = processStructure(type, infile)
except UnicodeDecodeError as e:
    print ("ERROR: Binary data found in the file.  Is it a CSV file?  Or the raw XLS?")
    print (e)
    print (line)
    print (fields)
    exit(1)
    
if (not type == "CANHealth" and not type == "CANWOD"):
    if (commonStructure == ""):
        print ("ERROR: No data found in the file.  Is it a CSV file?")
        exit(1)
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
if (not type == "CANHealth" and not type == "CANWOD"):
    outfile.write(commonStructure)
outfile.write(typeStructure) 
outfile.close()
infile.close()
