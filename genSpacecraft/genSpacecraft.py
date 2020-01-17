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
            print('Includng IFDEF: ' + define[1])
            DEFIF = True
        else:
            print('Excluding IFDEF: ' + define[1])
            DEFIF = False
        #print ('DEFIF set to: ' + str(DEFIF))
    return DEFIF

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

if len(sys.argv) < 4:
    print ('Usage: genSpacecraft <FOXID> <rt|max|min|rad|wod|CAN|CANWOD> <fileName.csv>')
    print ('Generate the spacecraft files needed for FoxTelem from the csv file that defines the downlink specification')
    sys.exit(1)

foxId = sys.argv[1]    
type = sys.argv[2]
fileName = sys.argv[3]
DEFINE_KEYWORDS = []
if len(sys.argv) > 4:
    DEFINES = sys.argv[4] # used for ifdef statements. Can be multiple words.
    DEFINE_KEYWORDS = DEFINES.split(' ') # make a list of any words
print ("defines " , DEFINE_KEYWORDS)
outFileName = foxId + '_rttelemetry.csv'
commonStructure = ""
common2Structure = ""
DEFIF = True # true if we out outside ifdef or inside a valid ifdef

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
            DEFIF = setIfDef(line)
            fields = line.split(',')
            # make sure this is not a comment row or excluded by ifdef
            if (DEFIF and "endif" not in fields[0] and "ifdef" not in fields[0] and "//" not in fields[0]):
                if ("Structure:" in fields[0]):
                    if ("header" not in fields[1]):
                        if (not type.lower() == "rad" and not type.lower() == "canhealth" and not type.lower() == "canwod"):
                            if ("commonDownlink" in fields[1]):
                                print("COMMON:" + fields[0] + " " + fields[1])
                                commonStructure = processStructure(type, infile)
                            if ("common2Downlink" in fields[1]):
                                print("COMMON2:" + fields[0] + " " + fields[1])
                                common2Structure = processStructure(type, infile)
                        if (type in fields[1]):
                            print("TYPE: " + fields[0] + " " + fields[1])
                            typeStructure = processStructure(type, infile)
except UnicodeDecodeError as e:
    print ("ERROR: Binary data found in the file.  Is it a CSV file?  Or the raw XLS?")
    print (e)
    print (line)
    print (fields)
    exit(1)
    
if (not type.lower() == "rad" and not type.lower() == "canhealth" and not type.lower() == "canwod"):
    if (commonStructure == ""):
        print ("ERROR: No data found for common structure in the file.  Is it a CSV file?")
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
if (common2Structure != ""):
    outfile.write(common2Structure)
outfile.write(typeStructure) 
outfile.close()
infile.close()
