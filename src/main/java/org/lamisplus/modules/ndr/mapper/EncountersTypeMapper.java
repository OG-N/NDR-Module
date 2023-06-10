package org.lamisplus.modules.ndr.mapper;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lamisplus.modules.base.domain.dto.ApplicationCodesetDTO;
import org.lamisplus.modules.base.service.ApplicationCodesetService;
import org.lamisplus.modules.hiv.domain.entity.ArtPharmacy;
import org.lamisplus.modules.hiv.domain.entity.Regimen;
import org.lamisplus.modules.hiv.domain.entity.RegimenType;
import org.lamisplus.modules.hiv.repositories.ArtPharmacyRepository;
import org.lamisplus.modules.ndr.domain.PatientDemographics;
import org.lamisplus.modules.ndr.domain.dto.ARTClinicalInfo;
import org.lamisplus.modules.ndr.repositories.NdrXmlStatusRepository;
import org.lamisplus.modules.ndr.schema.CodedSimpleType;
import org.lamisplus.modules.ndr.schema.EncountersType;
import org.lamisplus.modules.ndr.schema.HIVEncounterType;
import org.lamisplus.modules.ndr.service.NDRCodeSetResolverService;
import org.lamisplus.modules.ndr.service.NDRService;
import org.lamisplus.modules.ndr.utility.DateUtil;
import org.lamisplus.modules.patient.domain.entity.Person;
import org.lamisplus.modules.patient.domain.entity.Visit;
import org.lamisplus.modules.patient.repository.PersonRepository;
import org.lamisplus.modules.patient.repository.VisitRepository;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeConfigurationException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class EncountersTypeMapper {
    private final VisitRepository visitRepository;
    private final NdrXmlStatusRepository ndrXmlStatusRepository;
    
    private final ArtPharmacyRepository pharmacyRepository;
    private final NDRCodeSetResolverService ndrCodeSetResolverService;
    private final PersonRepository personRepository;

    private final ApplicationCodesetService applicationCodesetService;
    
    private final  PregnancyStatus pregnancyStatus;
    List<ARTClinicalInfo> clinicalInfoList;
    List<ARTClinicalInfo> clinicalInfos;
    
    private List<ARTClinicalInfo> getClinicalInforUUID(String personUuid)
    {
    	List<ARTClinicalInfo> clinicalInfos = new ArrayList<>();
    	clinicalInfos.forEach(clinicalInfo -> {
    		if(clinicalInfo.getpersonUuid() == personUuid) clinicalInfos.add(clinicalInfo);
     });
    	return clinicalInfos;
    }
    

    public EncountersType encounterType(PatientDemographics demographics) {
    	//fetch clinical data for all clients
    	if(clinicalInfos == null)
    	{
    		log.info("Retrieving clinical data for all eligibles");
    		clinicalInfos = ndrXmlStatusRepository.getClinicalInfoByUUIDIn(NDRService.personUuids);
    	}
    	else log.info("Re-using existing clinical data");
        EncountersType encountersType = new EncountersType ();
          if(demographics != null){
              Optional<Person> person = personRepository.findById(demographics.getId());
              List<HIVEncounterType> hivEncounter = encountersType.getHIVEncounter ();
              clinicalInfoList = clinicalInfos.size() > 0 ? getClinicalInforUUID(demographics.getPersonUuid()) :
                      ndrXmlStatusRepository.getClinicalInfoByPersonUuid(demographics.getPersonUuid());
              log.info ("encounter list size {}",  clinicalInfoList.size());
              
              clinicalInfoList.forEach (
                    artClinical -> {
                        HIVEncounterType hivEncounterType = new HIVEncounterType ();
                        hivEncounterType.setVisitID (artClinical.getclinicalUuid());
                        processAndSetVisitDate (artClinical, hivEncounterType);
                        processAndSetNextAppointment (artClinical, hivEncounterType);
                        processAndSetWeightAndHeight (hivEncounterType, artClinical);
                        processAndSetBloodPressure (hivEncounterType, artClinical);
                        processAndSetWhoStageAndFunctionalStatus (artClinical, hivEncounterType);
                        person.ifPresent(value -> processClinicalEncounterRegimens(value, artClinical, hivEncounterType));
                        processAndSetTBStatus (demographics.getPersonUuid(), hivEncounterType);
                        Map<String, Object> status =
                                pregnancyStatus.getPregnancyStatus(demographics.getPersonUuid());
                        if(demographics.getSex() != null && demographics.getSex().contains("F")) {
                            hivEncounterType.setEDDandPMTCTLink((String) status.get("status"));
                        }
                        hivEncounter.add (hivEncounterType);
                    });
              if(hivEncounter.isEmpty()) return null;

        }
        return encountersType;
    }
    
    
    public EncountersType encounterType(PatientDemographics demographics, LocalDateTime lastDateTime) {
    	if(this.clinicalInfos == null)
    	{
    		log.info("Retrieving clinical data for all eligibles");
    		clinicalInfos = ndrXmlStatusRepository.getClinicalInfoByUUIDIn(NDRService.personUuids);
    	}
    	else log.info("Re-using existing clinical data");
        EncountersType encountersType = new EncountersType ();
        if(demographics != null){
            Optional<Person> person = personRepository.findById(demographics.getId());
            List<HIVEncounterType> hivEncounter = encountersType.getHIVEncounter ();
           // List<ARTClinicalInfo> clinicalInfoList =
                 //   ndrXmlStatusRepository.getClinicalInfoByPersonUuidByLastModifiedDate(demographics.getPersonUuid(), lastDateTime);
            this.clinicalInfoList = this.clinicalInfos.size() > 0 ? getClinicalInforUUID(demographics.getPersonUuid()) :
                ndrXmlStatusRepository.getClinicalInfoByPersonUuidByLastModifiedDate(demographics.getPersonUuid(),lastDateTime);
            log.info ("encounter list size {}",  clinicalInfoList.size());
            clinicalInfoList.forEach (
                    artClinical -> {
                        HIVEncounterType hivEncounterType = new HIVEncounterType ();
                        hivEncounterType.setVisitID (artClinical.getclinicalUuid());
                        processAndSetVisitDate (artClinical, hivEncounterType);
                        processAndSetNextAppointment (artClinical, hivEncounterType);
                        processAndSetWeightAndHeight (hivEncounterType, artClinical);
                        processAndSetBloodPressure (hivEncounterType, artClinical);
                        processAndSetWhoStageAndFunctionalStatus (artClinical, hivEncounterType);
                        person.ifPresent(value -> processClinicalEncounterRegimens(value, artClinical, hivEncounterType));
                        processAndSetTBStatus (demographics.getPersonUuid(), hivEncounterType);
                        Map<String, Object> status =
                                pregnancyStatus.getPregnancyStatus(demographics.getPersonUuid());
                        if(demographics.getSex() != null && demographics.getSex().contains("F")) {
                            hivEncounterType.setEDDandPMTCTLink((String) status.get("status"));
                        }
                        hivEncounter.add (hivEncounterType);
                    });
            if(hivEncounter.isEmpty()) return null;
            
        }
        return encountersType;
    }

    private void processAndSetTBStatus(String personUuid, HIVEncounterType hivEncounterType) {
    
        Optional<String> tbStatusOptional = 
                ndrXmlStatusRepository.getTbStatusByPersonUuid(personUuid);
         tbStatusOptional.ifPresent (tbStatus -> {
             log.debug("tbStatus: {} " + tbStatus);
            Optional<String> ndrTBStatus = ndrCodeSetResolverService.getNDRCodeSetCode ("TB_STATUS", tbStatus);
            ndrTBStatus.ifPresent (hivEncounterType::setTBStatus);
        });
    }


    private void processClinicalEncounterRegimens(Person person, ARTClinicalInfo artClinical, HIVEncounterType hivEncounterType) {
        Optional<Visit> visitOptional = visitRepository.findById(artClinical.getClinicId());
        if(visitOptional.isPresent()) {
            List<ArtPharmacy> pharmacies =
                    pharmacyRepository.getArtPharmaciesByVisitAndPerson(visitOptional.get(), person);
            log.info("pharmacy list encounter {}", pharmacies.size());
            List<Long> regimenTypeIds = new ArrayList<>(Arrays.asList(1L, 2L, 3L, 4L, 14L));
            List<Long> cotrimoxazoleTypeIds = new ArrayList<>(Collections.singletonList(8L));
            pharmacies.forEach(
                    artPharmacy -> {
                        Set<Regimen> regimens = artPharmacy.getRegimens();
                        log.info("Regimen list = {}", regimens.size());
                        handleARVs(hivEncounterType, regimenTypeIds, regimens);
                        handleCotrimoxazole(hivEncounterType, cotrimoxazoleTypeIds, regimens);
                    }
            );
        }
    }
    
   
    private void handleARVs(HIVEncounterType hivEncounterType, List<Long> regimenTypeIds, Set<Regimen> regimens) {
        regimens.stream ()
                .filter (regimen -> regimenTypeIds.contains (regimen.getRegimenType ().getId ()))
                .forEach (regimen -> {
                    log.info ("ndrRegimenSystemDescription {}", regimen.getDescription ());
                   
                    Optional<CodedSimpleType> ndrCodeSet = ndrCodeSetResolverService.getRegimen (regimen.getDescription ());
                    if (ndrCodeSet.isPresent ()) {
                        System.out.println("ndr "+ndrCodeSet.get().getCodeDescTxt());
                        ndrCodeSet.ifPresent (hivEncounterType::setARVDrugRegimen);
                    } else {
                        RegimenType regimenType = regimen.getRegimenType ();
                        if (regimenType != null) {
                            String others = "Others" + "_" + regimenType.getId ();
                            log.info ("others {}", others);
                            Optional<CodedSimpleType> ndrCodeSet2 = ndrCodeSetResolverService.getSimpleCodeSet (others);
                            ndrCodeSet2.ifPresent (hivEncounterType::setARVDrugRegimen);

                        }
                    }

                });
    }
    
    private void handleCotrimoxazole (HIVEncounterType hivEncounterType, List<Long> cotrimoxazoleTypeIds, Set<Regimen> regimens) {
        regimens.stream ()
                .filter (regimen -> cotrimoxazoleTypeIds.contains (regimen.getRegimenType ().getId ()))
                .forEach (regimen -> {
                    RegimenType regimenType = regimen.getRegimenType ();
                    String composition = regimen.getComposition ();
                    if (regimenType != null && composition != null) {
                        String description = regimenType.getDescription ();
                        log.info ("cotrimoxazole {}", description);
                        Optional<CodedSimpleType> codedSimpleType =
                                ndrCodeSetResolverService.getNDRCodeSet ("REGIMEN_TYPE", description);
                        codedSimpleType.ifPresent (hivEncounterType::setCotrimoxazoleDose);
                    }
                });
    }
    
    
    private void processAndSetVisitDate(ARTClinicalInfo artClinical, HIVEncounterType hivEncounterType) {
        LocalDate visitDate = artClinical.getVisitDate ();
        if (visitDate != null) {
            try {
                hivEncounterType.setVisitDate (DateUtil.getXmlDate (Date.valueOf (visitDate)));
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace ();
            }
        }
    }


    private void processAndSetBloodPressure(HIVEncounterType hivEncounterType, ARTClinicalInfo vitalSign) {
        //resolving null pointer on blood pressure
        double bloodPressure = 0.0;
        Double systolic = vitalSign.getSystolic();
        Double diastolic = vitalSign.getDiastolic();
        if(systolic != null && diastolic != null) {
            bloodPressure = (systolic / diastolic);
        }
        if (bloodPressure > 0) {
            int systolic1 = systolic.intValue ();
            int diastolic1 = diastolic.intValue ();
            String bloodPressureValue = systolic1 + "/" + diastolic1;
            hivEncounterType.setBloodPressure (bloodPressureValue);
    
        }
        log.info("blood pressure {}", bloodPressure);
    }

    private void processAndSetNextAppointment(ARTClinicalInfo artClinical, HIVEncounterType hivEncounterType) {
        LocalDate nextAppointment = artClinical.getNextAppointment ();
        if (nextAppointment != null) {
            try {
                hivEncounterType.setNextAppointmentDate (DateUtil.getXmlDate (Date.valueOf (nextAppointment)));
            } catch (DatatypeConfigurationException e) {
               e.printStackTrace ();
            }
        }
    }

    private void processAndSetWeightAndHeight(HIVEncounterType hivEncounterType, ARTClinicalInfo vitalSign) {
        int  bodyWeight = vitalSign.getBodyWeight() == null  ? 0 : vitalSign.getBodyWeight().intValue();
        int  height = vitalSign.getHeight() == null ? 0 : vitalSign.getHeight().intValue();
        
        if (bodyWeight > 0) {
            hivEncounterType.setWeight (bodyWeight);
        }
        if (bodyWeight > 200) {
            hivEncounterType.setWeight (bodyWeight/10);
        }
        if (height > 0) {
            hivEncounterType.setChildHeight (height);
        }
        if (height > 200) {
            hivEncounterType.setChildHeight (height/10);
        }
    }


    private void processAndSetWhoStageAndFunctionalStatus(ARTClinicalInfo artClinical, HIVEncounterType hivEncounterType) {
        if (artClinical.getFunctionalStatusId() != null && artClinical.getFunctionalStatusId() > 0) {
            ApplicationCodesetDTO functionalStatus = applicationCodesetService.getApplicationCodeset(artClinical.getFunctionalStatusId());
            if (functionalStatus != null) {
                Optional<String> functionalStatusCodeSet =
                        ndrCodeSetResolverService.getNDRCodeSetCode("FUNCTIONAL_STATUS", functionalStatus.getDisplay());
                functionalStatusCodeSet.ifPresent(hivEncounterType::setFunctionalStatus);
        
            }
        }
        if (artClinical.getWhoStagingId() != null && artClinical.getWhoStagingId() > 0) {
            ApplicationCodesetDTO WHOStageCode = applicationCodesetService.getApplicationCodeset(artClinical.getWhoStagingId());
            if (WHOStageCode != null) {
                Optional<String> whoStageCodeSet =
                        ndrCodeSetResolverService.getNDRCodeSetCode("WHO_STAGE", WHOStageCode.getDisplay());
                whoStageCodeSet.ifPresent(hivEncounterType::setWHOClinicalStage);
            }
        }else {
            if(artClinical.getClinicalStageId() != null && artClinical.getClinicalStageId() > 0){
                ApplicationCodesetDTO WHOStageCode = applicationCodesetService.getApplicationCodeset(artClinical.getClinicalStageId());
                if (WHOStageCode != null) {
                    Optional<String> whoStageCodeSet =
                            ndrCodeSetResolverService.getNDRCodeSetCode("WHO_STAGE", WHOStageCode.getDisplay());
                    whoStageCodeSet.ifPresent(hivEncounterType::setWHOClinicalStage);
                }
            }
        }
    }
}
