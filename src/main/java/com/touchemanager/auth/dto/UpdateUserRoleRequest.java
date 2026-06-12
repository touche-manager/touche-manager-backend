package com.touchemanager.auth.dto;

import com.touchemanager.auth.entity.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {

    public enum Action { ADD, REMOVE }

    @NotNull(message = "Role is required")
    private RoleName role;

    /** ADD grants the role, REMOVE revokes it. Defaults to ADD. */
    private Action action = Action.ADD;
}
