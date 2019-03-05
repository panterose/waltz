/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017 Waltz open source project
 * See README.md for more information
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.khartec.waltz.service.measurable_relationship;

import com.khartec.waltz.data.EntityReferenceNameResolver;
import com.khartec.waltz.data.entity_relationship.EntityRelationshipDao;
import com.khartec.waltz.model.*;
import com.khartec.waltz.model.changelog.ImmutableChangeLog;
import com.khartec.waltz.model.entity_relationship.*;
import com.khartec.waltz.service.changelog.ChangeLogService;
import com.khartec.waltz.service.entity_relationship.EntityRelationshipUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.common.ListUtilities.map;
import static com.khartec.waltz.common.ListUtilities.newArrayList;
import static java.lang.String.format;

@Service
public class MeasurableRelationshipService {

    private final EntityRelationshipDao entityRelationshipDao;
    private final ChangeLogService changeLogService;
    private final EntityReferenceNameResolver entityReferenceNameResolver;


    @Autowired
    public MeasurableRelationshipService(EntityRelationshipDao entityRelationshipDao,
                                         EntityReferenceNameResolver entityReferenceNameResolver,
                                         ChangeLogService changeLogService) {
        checkNotNull(entityRelationshipDao, "entityRelationshipDao cannot be null");
        checkNotNull(entityReferenceNameResolver, "entityReferenceNameResolver cannot be null");
        checkNotNull(changeLogService, "changeLogService cannot be null");
        this.entityRelationshipDao = entityRelationshipDao;
        this.entityReferenceNameResolver = entityReferenceNameResolver;
        this.changeLogService = changeLogService;
    }


    public Collection<EntityRelationship> findForEntityReference(EntityReference entityReference) {
        checkNotNull(entityReference, "entityReference cannot be null");
        return entityRelationshipDao
                .findRelationshipsInvolving(entityReference);
    }


    public Map<EntityKind, Integer> tallyForEntityReference(EntityReference entityReference) {
        checkNotNull(entityReference, "entityReference cannot be null");
        return entityRelationshipDao
                .tallyRelationshipsInvolving(entityReference);
    }


    public boolean remove(EntityRelationshipKey command, String username) {
        boolean result = entityRelationshipDao.remove(command);
        if (result) {
            logRemoval(command, username);
        }
        return result;
    }


    public boolean create(String userName,
                          EntityReference entityRefA,
                          EntityReference entityRefB,
                          RelationshipKind relationshipKind,
                          String description) {

        Optional<EntityRelationshipKey> entityRelationshipKey =
                EntityRelationshipUtilities.mkEntityRelationshipKey(entityRefA, entityRefB, relationshipKind, true);

        EntityRelationship relationship = entityRelationshipKey
                .map(erKey -> ImmutableEntityRelationship.builder()
                        .a(erKey.a())
                        .b(erKey.b())
                        .relationship(relationshipKind)
                        .description(description)
                        .lastUpdatedBy(userName)
                        .build())
                .orElseThrow(() -> new IllegalArgumentException("Entity relationship type " + relationshipKind
                        + " cannot be created between " + entityRefA
                        + " and " + entityRefB));


        boolean result = entityRelationshipDao.create(relationship);
        if (result) {
            logAddition(relationship);
        }
        return result;
    }


    public boolean update(EntityRelationshipKey key, UpdateEntityRelationshipParams params, String username) {
        boolean result = entityRelationshipDao.update(key, params, username);
        if (result) {
            logUpdate(key, params, username);
        }
        return result;
    }


    // --- helpers ---

    private void logUpdate(EntityRelationshipKey key, UpdateEntityRelationshipParams params, String username) {
        List<String> niceNames = resolveNames(
                key.a(),
                key.b());

        String paramStr = "";
        paramStr += params.relationshipKind() != null
                ? " Relationship: " + params.relationshipKind()
                : "";

        paramStr += params.description() != null
                ? " Updated description"
                : "";

        String msg = format(
                "Updated explicit relationship from: '%s', to: '%s', with params: '%s'",
                niceNames.get(0),
                niceNames.get(1),
                paramStr);

        writeLog(
                Operation.UPDATE,
                key.a(),
                msg,
                username);
        writeLog(
                Operation.UPDATE,
                key.b(),
                msg,
                username);
    }


    private void logRemoval(EntityRelationshipKey key, String username) {
        List<String> niceNames = resolveNames(
                key.a(),
                key.b());

        String msg = format(
                "Removed explicit relationship from: '%s', to: '%s'",
                niceNames.get(0),
                niceNames.get(1));

        writeLog(
                Operation.REMOVE,
                key.a(),
                msg,
                username);
        writeLog(
                Operation.REMOVE,
                key.b(),
                msg,
                username);
    }


    private void logAddition(EntityRelationship relationship) {
        List<String> niceNames = resolveNames(
                relationship.a(),
                relationship.b());

        String msg = format(
                "Added explicit relationship from: '%s', to: '%s'",
                niceNames.get(0),
                niceNames.get(1));

        writeLog(
                Operation.ADD,
                relationship.a(),
                msg,
                relationship.lastUpdatedBy());
        writeLog(
                Operation.ADD,
                relationship.b(),
                msg,
                relationship.lastUpdatedBy());
    }


    private void writeLog(Operation op, EntityReference a, String message, String username) {
        ImmutableChangeLog logEntry = ImmutableChangeLog.builder()
                .severity(Severity.INFORMATION)
                .operation(op)
                .parentReference(a)
                .userId(username)
                .message(message)
                .build();
        changeLogService.write(logEntry);
    }


    private List<String> resolveNames(EntityReference... refs) {
        return map(
                entityReferenceNameResolver.resolve(newArrayList(refs)),
                r -> r.name().orElse("?"));
    }

}
