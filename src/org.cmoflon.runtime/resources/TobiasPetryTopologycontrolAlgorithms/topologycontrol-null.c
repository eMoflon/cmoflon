#include <stdlib.h>
#include "contiki.h"

#include "../../app-conf.h"
#include "../../lib/boot.h"

#if COMPONENT_TOPOLOGYCONTROL == COMPONENT_TOPOLOGYCONTROL_NULL
PROCESS(component_topologycontrol, "topologycontrol: null");
PROCESS_THREAD(component_topologycontrol, ev, data) {
	PROCESS_BEGIN();

	BOOT_COMPONENT_WAIT(component_topologycontrol);

	PROCESS_END();
}
#endif
