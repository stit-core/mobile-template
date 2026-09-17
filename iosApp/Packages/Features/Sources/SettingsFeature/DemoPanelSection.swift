import DesignSystem
import SharedBridge
public import SwiftUI

// Панель demo-режима: только при API_MODE=mock (схема Demo), сценарии — фикстуры mock/.
public struct DemoPanelSection: View {
  private let model: DemoPanelModel

  public init(model: DemoPanelModel) {
    self.model = model
  }

  public var body: some View {
    let state = model.state
    Section(header: Text("settings.demo", bundle: .module)) {
      Picker(selection: Binding(get: { state.scenario }, set: model.select)) {
        ForEach(state.scenarios, id: \.self) { scenario in
          Text(scenario.name.lowercased()).tag(scenario)
        }
      } label: {
        Text("settings.demo.scenario", bundle: .module)
      }
      .accessibilityIdentifier("demo.scenario")
      Stepper(value: Binding(get: { state.latencyMs }, set: model.setLatency), in: 0...5000, step: 250) {
        LabeledContent {
          Text("\(state.latencyMs) ms")
        } label: {
          Text("settings.demo.latency", bundle: .module)
        }
      }
      .accessibilityIdentifier("demo.latency")
    }
  }
}
