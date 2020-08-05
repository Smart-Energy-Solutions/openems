package io.openems.edge.simulator.meter.grid.reacting;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.GridMeter.Reacting", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID" //
		})
public class GridMeter extends AbstractOpenemsComponent implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	// private final Logger log = LoggerFactory.getLogger(GridMeter.class);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Reference
	protected ConfigurationAdmin cm;

	private final CopyOnWriteArraySet<ManagedSymmetricEss> symmetricEsss = new CopyOnWriteArraySet<>();

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	protected void addEss(ManagedSymmetricEss ess) {
		this.symmetricEsss.add(ess);
		ess.getActivePowerChannel().onSetNextValue(this.updateChannelsCallback);
	}

	protected void removeEss(ManagedSymmetricEss ess) {
		ess.getActivePowerChannel().removeOnSetNextValueCallback(this.updateChannelsCallback);
		this.symmetricEsss.remove(ess);
	}

	private final CopyOnWriteArraySet<SymmetricMeter> symmetricMeters = new CopyOnWriteArraySet<>();

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Simulator.GridMeter.Reacting)))")
	protected void addMeter(SymmetricMeter meter) {
		this.symmetricMeters.add(meter);
		meter.getActivePowerChannel().onSetNextValue(this.updateChannelsCallback);
	}

	protected void removeMeter(SymmetricMeter meter) {
		meter.getActivePowerChannel().removeOnSetNextValueCallback(this.updateChannelsCallback);
		this.symmetricMeters.remove(meter);
	}

	private final Consumer<Value<Integer>> updateChannelsCallback = (value) -> {
		Integer sum = null;

		for (ManagedSymmetricEss ess : this.symmetricEsss) {
			sum = subtract(sum, ess.getActivePowerChannel().getNextValue().get());
		}
		for (SymmetricMeter sm : this.symmetricMeters) {
			try {
				switch (sm.getMeterType()) {
				case CONSUMPTION_METERED:
				case GRID:
					// ignore
					break;
				case CONSUMPTION_NOT_METERED:
					sum = add(sum, sm.getActivePowerChannel().getNextValue().get());
					break;
				case PRODUCTION:
				case PRODUCTION_AND_CONSUMPTION:
					sum = subtract(sum, sm.getActivePowerChannel().getNextValue().get());
					break;
				}
			} catch (NullPointerException e) {
				// ignore
			}
		}

		this._setActivePower(sum);

		Integer simulatedActivePowerByThree = TypeUtils.divide(sum, 3);
		this._setActivePowerL1(simulatedActivePowerByThree);
		this._setActivePowerL2(simulatedActivePowerByThree);
		this._setActivePowerL3(simulatedActivePowerByThree);
	};

	private static Integer add(Integer sum, Integer activePower) {
		if (activePower == null && sum == null) {
			return null;
		} else if (activePower == null) {
			return sum;
		} else if (sum == null) {
			return activePower;
		} else {
			return sum + activePower;
		}
	}

	private static Integer subtract(Integer sum, Integer activePower) {
		if (activePower == null && sum == null) {
			return null;
		} else if (activePower == null) {
			return sum;
		} else if (sum == null) {
			return activePower * -1;
		} else {
			return sum - activePower;
		}
	}

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public GridMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
	}
}
