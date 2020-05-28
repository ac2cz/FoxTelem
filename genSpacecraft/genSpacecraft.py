# Generate the spacecraft files needed for FoxTelem from the csv file that defines
# the Downlink Spec
# g0kla@arrl.net

import sys

lineNum = 0;


def setIfDef(line):
    "This checks to see if this line is an IFDEF with a valid keyword"
    global DEFIF #otherwise the assignment below automagically creates a local variable
    if ("endif" in line):
        DEFIF = True
        #print('ENDIF: ' + line)
        #print ('DEFIF set to: ' + str(DEFIF))
        return DEFIF
    if ("ifdef" in line):
        fields = line.split(",")
        define = fields[0].split(" ")
        #print('define : ' , define)
        if (len(define) > 1 and (define[1] in DEFINE_KEYWORDS)):
            #print('Includng IFDEF: ' + define[1])
            DEFIF = True
        else:
            #print('Excluding IFDEF: ' + define[1])
            DEFIF = False
        #print ('DEFIF set to: ' + str(DEFIF))
    return DEFIF

def processColumns(fields, columns):
    global lineNum #otherwise the assignment below automagically creates a local lineNum variable
    outputLine = ""
    outputLine += str(lineNum)
    lineNum = lineNum + 1
    outputLine += ','
    outputLine += type
    for col in columns:
        if (fields[col] == ''):
            fields[col] = '-'
        if (fields[col] == 'battAv'):
            fields[col] = 'BATT_A_V'
        if (fields[col] == 'battBv'):
            fields[col] = 'BATT_B_V'
        if (fields[col] == 'battCv'):
            fields[col] = 'BATT_C_V'
        outputLine += ','
        if (col == 13):
            if (type == 'maxDownlink' or type == 'minDownlink'):
                # If this shows all values no need for MAX/MIN to have the row
                if ('3' in fields[16] or '4' in fields[16]):
                    fields[13] = 'NONE'
        outputLine += fields[col].rstrip('\n')
    outputLine += '\n'
    return outputLine

def processStructure(type, file):
    "This processes a structure section of the document"
    global lineNum #otherwise the assignment below automagically creates a local lineNum variable
    structure = ""
    columns = [2, 3, 7, 6, 13, 14, 15, 16, 1, 8]
    for line in file:
        DEFIF = setIfDef(line)
        fields = line.split(',')
        if ('END' in fields[0]):
            return structure
        if (DEFIF and "endif" not in fields[0] and "ifdef" not in fields[0] and "//" not in fields[0]):
            if (int(fields[3]) > 32):
                numOfRows = int(int(fields[3]) / 8)
                fieldName = fields[2]
                print("Breaking long field: " + fields[2] + " into " + str(numOfRows)+ " bytes")
                fields[3] = "8"  # make this a repeating 8 bit field
                for i in range(0, numOfRows):
                    fields[2] = fieldName + str(i)  # name this as a repeating 8 bit field
                    structure += processColumns(fields, columns)
            else:
                structure += processColumns(fields, columns)

if len(sys.argv) < 5:
    print ('Usage: genSpacecraft <FOXID> <rt|max|min|rad|exp|wod|can|canwod> <fileName.csv> <include sections> <ifdef keywords>')
    print ('Generate the spacecraft files needed for FoxTelem from the csv file that defines the downlink specification')
    sys.exit(1)

foxId = sys.argv[1]    
type = sys.argv[2]
fileName = sys.argv[3]
INCLUDE_KEYWORDS = []
INCLUDES = sys.argv[4] # used for include sections. Can be multiple words which need to be in quotes on command line.
INCLUDE_KEYWORDS = INCLUDES.split(' ') # make a list of any words
#print ("includes " , INCLUDE_KEYWORDS)
DEFINE_KEYWORDS = []
if len(sys.argv) > 5:
    DEFINES = sys.argv[5] # used for ifdef statements. Can be multiple words which need to be in quotes on command line.
    DEFINE_KEYWORDS = DEFINES.split(' ') # make a list of any words
#print ("defines " , DEFINE_KEYWORDS)
outFileName = foxId + '_rttelemetry.csv'
commonStructure = ""
common2Structure = ""
DEFIF = True # true if we out outside ifdef or inside a valid ifdef

if (type.lower() == "header"):
    outFileName = foxId + '_header.csv'
elif (type.lower() == "max"):
    outFileName = foxId + '_maxtelemetry.csv'
    type = "maxDownlink"    
elif (type.lower() == "min"):
    outFileName = foxId + '_mintelemetry.csv'
    type = "minDownlink"    
elif (type.lower() == "can"):
    outFileName = foxId + '_exptelemetry.csv'
    type = "CANHealth"    
elif (type.lower() == "canwod"):
    outFileName = foxId + '_wodexptelemetry.csv'
    type = "CANWOD"    
elif (type.lower() == "rt"):
    type = "realtimeDownlink"    
else:
    outFileName = foxId + '_' + type + 'telemetry.csv'

print ('Processing '+ type + ' from file: ' + fileName)


line = ""
fields = []
# open the infile and read all the content in as a set of lines
try:
    with open(fileName) as infile:
        for line in infile:
            DEFIF = setIfDef(line)
            fields = line.split(',')
            # make sure this is not a comment row or excluded by ifdef or a blank row with no fields
            if (DEFIF and "endif" not in fields[0] and "ifdef" not in fields[0] and "//" not in fields[0]):
                if ("Structure:" in fields[0]):
                    #print("STRUCT:" + fields[0] + "," + fields[1])
                    includes = [x.strip() for x in fields[0].split(':')] # this also strips spaces
                    #print("include:" + includes[0] + "," + includes[1])
                    if (includes[1] in INCLUDE_KEYWORDS):
                        print("...including:" + fields[0] + "," + fields[1])
                        structureChunk = processStructure(type, infile)
                        #print(structureChunk)
                        commonStructure += structureChunk
except UnicodeDecodeError as e:
    print ("ERROR: Binary data found in the file.  Is it a CSV file?  Or the raw XLS?")
    print (e)
    print (line)
    print (fields)
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

if (commonStructure != ""):
    outfile.write(commonStructure)
infile.close()
outfile.close()
