package aqua.client;

import aqua.common.msgtypes.Token;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleController implements ActionListener {
    private final String fishId;
    private final TankModel tankModel;

    public ToggleController(String fishId, TankModel tankModel) {
        this.fishId = fishId;
        this.tankModel = tankModel;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        tankModel.locateFishGlobally(fishId);
    }
}
