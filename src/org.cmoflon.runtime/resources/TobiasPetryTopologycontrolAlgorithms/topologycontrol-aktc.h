#ifndef __TOPOLOGYCONTROL_AKTC_H_
#define __TOPOLOGYCONTROL_AKTC_H_

#include "../../app-conf.h"
#include "../../lib/neighbors.h"

#ifndef COMPONENT_TOPOLOGYCONTROL_AKTC_K
#define COMPONENT_TOPOLOGYCONTROL_AKTC_K 1.41
#endif

#ifndef COMPONENT_TOPOLOGYCONTROL_AKTC_UPDATEINTERVAL
#define COMPONENT_TOPOLOGYCONTROL_AKTC_UPDATEINTERVAL 300
#endif

int aktc_criteria(const neighbor_t *directhop, const neighbor_t *onehop, const neighbor_t *twohop);

#endif /* __TOPOLOGYCONTROL_AKTC_H_ */
