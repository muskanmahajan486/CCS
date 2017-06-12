package org.openremote.controllercommand.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="execute_device_controller_command")
public class ExecuteDeviceControllerCommand extends ControllerCommand {

    private String deviceName;
    private String commandName;
    private String parameter;

    public ExecuteDeviceControllerCommand() {
        super();
    }

    public ExecuteDeviceControllerCommand(Account account, ControllerCommandDTO.Type type, String deviceName, String commandName, String parameter) {
        super(account, type);
        this.deviceName = deviceName;
        this.commandName = commandName;
        this.parameter = parameter;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
}
