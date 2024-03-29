# Generate a C header files needed for spacecraft from the FoxTelem payload file
# g0kla@arrl.net

import sys

#
# Main
#
if len(sys.argv) < 3:
    print ('Usage: gen_header <name> <fileName.csv>')
    print ('Generate a spacecraft c header file from the FoxTelem payload file')
    sys.exit(1)
	
name = sys.argv[1]
fileName = sys.argv[2]
line = ""
fields = []
firstLine = True
num = 0

print("typedef struct __attribute__((__packed__)) {");
# open the infile and read all the content in as a set of lines
try:
    with open(fileName) as infile:
        for line in infile:
            if (firstLine) :
                firstLine = False
            else :
                fields = line.split(',')
                print('    unsigned int ' + fields[2] + ' : ' + fields[3] + ';')

    print('} ' + name + '_t;\n')

    #
    # Now generate the function that prints the layout
    #
    firstLine = True
    print('void print_'+name+'('+ name + '_t payload) {')
    print('    printf("RAW PAYLOAD: '+name+r'\n");') # use a raw string where we want the \n
    with open(fileName) as infile:
        for line in infile:
            if (firstLine) :
                firstLine = False
            else :
                fields = line.split(',')
                print('    printf("'+fields[10]+': %d  ",payload.'+fields[2]+');')
                num += 1
                if (num % 4 == 0) :
                    print(r'    printf("\n");') # print as a raw string to allow \n
    if (num %4 != 0) :
        print(r'    printf("\n");')
    print('}\n')

except UnicodeDecodeError as e:
    print ("ERROR: Binary data found in the file.  Is it a CSV file?")
    print (e)
    print (line)
    print (fields)
    exit(1)
