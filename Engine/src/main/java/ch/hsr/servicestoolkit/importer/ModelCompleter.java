package ch.hsr.servicestoolkit.importer;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.hsr.servicestoolkit.model.CouplingCriterionCharacteristic;
import ch.hsr.servicestoolkit.model.CouplingInstance;
import ch.hsr.servicestoolkit.model.CouplingType;
import ch.hsr.servicestoolkit.model.Model;
import ch.hsr.servicestoolkit.model.Nanoentity;
import ch.hsr.servicestoolkit.model.repository.CouplingCriterionCharacteristicRepository;
import ch.hsr.servicestoolkit.model.repository.CouplingCriterionRepository;
import ch.hsr.servicestoolkit.model.repository.CouplingInstanceRepository;
import ch.hsr.servicestoolkit.model.repository.NanoentityRepository;

@Component
public class ModelCompleter {

	private CouplingInstanceRepository couplingInstanceRepository;
	private CouplingCriterionRepository couplingCriterionRepository;
	private CouplingCriterionCharacteristicRepository characteristicRepository;
	private NanoentityRepository nanoentityRepository;

	private final Logger log = LoggerFactory.getLogger(ModelCompleter.class);

	@Autowired
	public ModelCompleter(final CouplingCriterionRepository couplingCriterionRepository, final CouplingCriterionCharacteristicRepository characteristicRepository,
			final CouplingInstanceRepository couplingInstanceRepository, final NanoentityRepository nanoentityRepository) {
		this.couplingCriterionRepository = couplingCriterionRepository;
		this.characteristicRepository = characteristicRepository;
		this.couplingInstanceRepository = couplingInstanceRepository;
		this.nanoentityRepository = nanoentityRepository;
	}

	/**
	 * creates characteristics instances for the default characteristic of a
	 * coupling criteria with all fields in the model for which no
	 * characteristic is defined.
	 */
	public void completeModelWithDefaultsForDistance(final Model model) {
		Set<Nanoentity> allFieldsInModel = nanoentityRepository.findByModel(model);
		Map<String, Set<CouplingInstance>> instancesByCriterion = couplingInstanceRepository.findByModelGroupedByCriterionFilteredByCriterionType(model, CouplingType.COMPATIBILITY);

		// For every criterion
		for (Entry<String, Set<CouplingInstance>> criterion : instancesByCriterion.entrySet()) {
			Set<Nanoentity> definedFields = criterion.getValue().stream().flatMap(instance -> instance.getAllNanoentities().stream()).collect(Collectors.toSet());
			// find missing fields which need to have an instance
			Set<Nanoentity> missingFields = allFieldsInModel.stream().filter(field -> !definedFields.contains(field)).collect(Collectors.toSet());

			if (!missingFields.isEmpty()) {
				CouplingCriterionCharacteristic defaultCharacteristic = characteristicRepository.readByCouplingCriterionAndIsDefault(couplingCriterionRepository.readByName(criterion.getKey()), true);
				Set<CouplingInstance> instances = couplingInstanceRepository.findByModelAndCharacteristic(model, defaultCharacteristic);
				CouplingInstance instance;
				if (instances.size() == 1) {
					instance = instances.iterator().next();
				} else if (instances.size() == 0) {
					instance = new CouplingInstance(defaultCharacteristic);
					model.addCouplingInstance(instance);
					instance.setName(defaultCharacteristic.getName());
					couplingInstanceRepository.save(instance);
				} else {
					throw new RuntimeException("only one instance per characteristic expected for distance criterion");
				}
				for (Nanoentity field : missingFields) {
					instance.addNanoentity(field);
				}
				log.info("Complete model with instance of characteristic {} of criterion {} and fields {}", defaultCharacteristic.getName(), criterion.getKey(), missingFields);
			}

		}
	}

}
