#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <math.h>
#include "flash.h"
#include "libusb.h"

static struct libusb_device_handle *fd = NULL;
static struct libusb_context *context = NULL; 
uint16_t vid = 0x0E6A, pid = 0x0325, vcp_pid = 0x0331;

static uint8_t *cmdbuf;
static uint8_t *databuf;
static char *mcu_name;
static uint32_t interruptInterfaceNo;
static uint32_t controlInterfaceNo;
static uint32_t voltage; 

struct device *found_device;

int flash_target(FILE *fp);
int mlink_init();
void mlink_exit();
int claim_interface(int idx);

int main(int argc, char *argv[]) {

	int ret;

    if (argc != 4) {
		printf("incomplete flash configuration directive\n");
		return ERROR_NO_CONFIG_FILE;
	}

	char *endptr;
	voltage = (uint32_t)strtoul(argv[2], &endptr, 16);
	printf("voltage = %x\n", voltage);
	
	mcu_name = argv[3];
	printf("mcu_name = %s\n", mcu_name);

	const char *path = argv[1];
	printf("flash image: %s \n", path);

	FILE *fp = fopen(path, "r");
	if (!fp) {
		printf("Error: Failed to open file %s\n", path);
		return ERROR_FAIL;
	}

	ret = mlink_init();
	if(ret != 0) {
		printf("Error: MLINK open failed, device not found\n");
		return ret;
	}

    ret = flash_target(fp);	
	mlink_exit();		

    return ret;
}

void init_buffer(void)  
{
	memset(cmdbuf, 0, MLINK_CMD_SIZE);
	memset(databuf, 0, MLINK_DATA_SIZE);
	buf_set_u32(cmdbuf, 0, 8, 0x55);	
}

uint8_t genTag(void) {
	srand((unsigned int)time(NULL));
	return (uint8_t)(rand() * clock());
}

int mlink_usb_control(libusb_device_handle *handle, uint16_t cmd, uint32_t addr, uint32_t len, uint32_t val) {	
	
	int ret;

	buf_set_u32(cmdbuf, 1 * 2 * 4, 8, genTag());
	buf_set_u32(cmdbuf, 2 * 2 * 4, 16, cmd);
	buf_set_u32(cmdbuf, 4 * 2 * 4, 32, addr);
	buf_set_u32(cmdbuf, 8 * 2 * 4, 32, len);
	buf_set_u32(cmdbuf, 12 * 2 * 4, 32, val);
	
	claim_interface(controlInterfaceNo);
	ret = libusb_control_transfer(handle, 0x21, 0x09, 0x0300, 0x0000, (char *)cmdbuf, 128, 1000);

	return ret;
}

int mlink_data_stage(libusb_device_handle *handle) {

	claim_interface(controlInterfaceNo);
	int ret = libusb_control_transfer(handle, 0xA1, 0x01, 0x0300, 0x0000, (char *)databuf, 128, 1000);
	return ret;
}

int mlink_usb_interrupt(void *handle, uint16_t cmd, uint32_t addr) {

	init_buffer();
	mlink_usb_control(fd, cmd, addr, 0, 0);

	int act_len;
	claim_interface(interruptInterfaceNo);
	int ret = libusb_interrupt_transfer(handle, 0x81, databuf, 64, &act_len, 1000);
	uint32_t status = le_to_h_u32(databuf);
	
	printf("code: %d ,act length: %d, status %08x\n", ret, act_len, status);
	return ERROR_OK;
}

struct device *find_device(struct device *devices, char* device_name)
{
    for (size_t i = 0; i < DEVICE_ARRAY_SIZE; i++) {
        if (!strcmp(devices[i].name, device_name)) {
            return &devices[i];
        }
    }

    return NULL;
}

int check_deviceId(struct device *device, uint32_t device_id)
{    
    return device->deviceId == device_id;
}

int ice_write_data(uint16_t cmd, uint8_t* buffer, uint32_t addr, uint32_t size)
{
	int dwResult;
	uint32_t checksum = 0;
	for (uint32_t i = 0; i < size; i++) {		
		checksum += buffer[i];
	}
	printf("checksum = %x\n", checksum);
	dwResult = mlink_usb_control(fd, cmd, addr, size, checksum);

	uint32_t offset = 0;
	uint32_t txSize = 0;
	uint8_t pTxBuffer[HID_PACKET_SIZE];
	int cnt = 0;
	if (dwResult != 0) {
		while (size) {
			memset(pTxBuffer, 0, HID_PACKET_SIZE);       
			txSize = size > HID_PACKET_SIZE ? HID_PACKET_SIZE : size;
			memcpy(pTxBuffer, buffer + offset, txSize);

			dwResult = libusb_control_transfer(fd, 0x21, 0x09, 0x0300, 0x0000, (char *)pTxBuffer, 128, 1000);
			size -= txSize;
			offset += txSize;
			cnt++;
		}
		int act_len;
		init_buffer();
		claim_interface(interruptInterfaceNo);
		dwResult = libusb_interrupt_transfer(fd, 0x81, databuf, 64, &act_len, 1000);									
		printf("ice_write_data return status: %x %d %d\n", databuf[0], databuf[2], databuf[3]);
	}

	return false;
}

int mlink_write_or(void)
{
	if(found_device != NULL) {
		return ice_write_data(PROGRAM_OR, found_device->pOR, 0x00, 0x20);
	} else {
		return ice_write_data(PROGRAM_OR, device_array[0].pOR, 0x00, 0x20);
	}
	return false;
}

double Hex2Bin(char** pHexData)
{
#define a2n(c) (((c) & 0x0f) + (((c) & 0x40) ? 9 : 0))
#pragma pack(push)
#pragma pack(1)
	struct
	{
		uint8_t cnt;
		uint16_t addr;
		uint8_t tag;
		uint8_t dat[256];
	}hdb;
#pragma pack(pop)
	double last = 0;
	uint32_t offset = 0;
	uint8_t* dump;
	char* seps = "\n\r";
	char* token;
	int end = false;
	uint8_t sum;	
    
    dump = (uint8_t*)calloc(256*1024, sizeof(uint8_t));
    memset(dump, 0xff, 256*1024);
	token = strtok(*pHexData, seps);
	while (!end && token)
	{
		if (*token++ == ':')
		{
			sum = 0;
			for (int i=0; token[i]; i+=2)
			{
				sum += (((uint8_t *)&hdb)[i/2] = (uint8_t)((a2n(token[i]) << 4) + a2n(token[i+1])));
			}
			if (sum != 0) break;
			__asm__(
				"movw %1, %%ax\n\t"
				"movb %%ah, %%cl\n\t"
				"movb %%al, %%ch\n\t"
				"movw %%cx, %0\n\t"
				:"=m"(hdb.addr)
				:"m"(hdb.addr)
				:"%ax", "%cx"
			);
			switch (hdb.tag)
			{
			case 0:
				memcpy(dump+hdb.addr+offset, hdb.dat, hdb.cnt);
        last = fmax(last, (double)(hdb.addr + offset + hdb.cnt));
				break;
			case 2:
			case 4:
				if ((hdb.cnt == 2) && (hdb.addr == 0))
				{
                    offset = ((hdb.dat[1] << 8) | hdb.dat[0]) << (1 << hdb.tag);
					dump = (uint8_t*)realloc(dump, offset+64*1024);
				}
				break;
			case 1:
			default:
				end = true;
				break;
			}
		}
		token = strtok(NULL, seps);
	}
    free(*pHexData);
    *pHexData = (char*)realloc(dump, last);
	return last;
}

int flash_target(FILE *fp) {
    

	// Get the file size
	fseek(fp, 0, SEEK_END);
	long fileSize = ftell(fp);
	rewind(fp);

	// Allocate memory for the file contents
	char* pHexData = (char*) malloc(fileSize + 1);
	if (!pHexData) {
		printf("Error: Failed to allocate memory for file contents\n");
		fclose(fp);
		return ERROR_FAIL;
	}

	// Read the file contents
	size_t bytesRead = fread(pHexData, 1, fileSize, fp);
	printf("read bytes: %zu \n", bytesRead);
	fclose(fp);

	found_device = find_device(device_array, mcu_name);
	if (found_device == NULL) {
		printf("Error: Can't find Device!, DeviceName = %s", mcu_name);
		return ERROR_FAIL;
	}

	mlink_usb_interrupt(fd, SET_OPEN_PULLUP_R, voltage);
	mlink_usb_interrupt(fd, SET_ICP_INIT, found_device->partNum);
	
	// Read Device ID
	mlink_usb_control(fd, READ_DUT_ID, found_device->partNum, 0x04, 0);	
	mlink_data_stage(fd);
	uint32_t deviceId = le_to_h_u32(databuf);	

	if (!check_deviceId(found_device, deviceId)) {
		printf("Error: DeviceID should be: %x, but get DeviceID: %x\n", found_device->deviceId, deviceId);
		return ERROR_FAIL;
	} else {
		printf("DeviceID %x is valid\n", deviceId);
	}

	mlink_usb_interrupt(fd, ERASE_CHIP, 0);
	mlink_write_or();
	
	// Convert the hex data to binary
	double binSize = Hex2Bin(&pHexData);
	printf("size = %lf \n", binSize);
	ice_write_data(PROGRAM_CODE, (uint8_t*)pHexData, 0x00, binSize);
	
	mlink_usb_control(fd, SET_ICE_END, 0, 0, 0);

	return ERROR_OK;
}

int claim_interface(int idx)
{
	int ret = libusb_claim_interface(fd, idx); 
	if(ret != LIBUSB_SUCCESS){
		printf("reclaim mlink interface: %d\n", idx);
		ret = libusb_detach_kernel_driver(fd, idx);
		ret = libusb_claim_interface(fd, idx);
		printf("reclaim code: %d\n", ret);
	}

	return ret;
}

int mlink_init(void) 
{
    int ret = libusb_init(&context);
    if (ret < 0) {
        printf("Failed to initialize libusb: %s\n", libusb_error_name(ret));
        return ret;
    }

	if (fd = libusb_open_device_with_vid_pid(context, vid, pid)) {
		controlInterfaceNo = MLINK_CONTROL_INTERFACE;
		interruptInterfaceNo = MLINK_INTERRUPT_INTERFACE;
		printf("vid = %04x, pid = %04x\n", vid , pid);
	} else if (fd = libusb_open_device_with_vid_pid(context, vid, vcp_pid)) {

		#ifdef _WIN32
			controlInterfaceNo = MLINK_VCP_WINDOWS_CONTROL_INTERFACE;
		#elif __linux__
			controlInterfaceNo = MLINK_VCP_LINUX_CONTROL_INTERFACE;
		#endif
				
		interruptInterfaceNo = MLINK_VCP_INTERRUPT_INTERFACE;
		printf("vid = %04x, pid = %04x\n", vid , vcp_pid);
	} else {
		printf("open failed, device not found\n");
		return ERROR_FAIL;
	}

	cmdbuf = malloc(MLINK_CMD_SIZE);
	databuf = malloc(MLINK_DATA_SIZE);
    
	return ret;
}

void mlink_exit() {
    libusb_release_interface(fd, 0);
    libusb_close(fd);
    libusb_exit(context);
}