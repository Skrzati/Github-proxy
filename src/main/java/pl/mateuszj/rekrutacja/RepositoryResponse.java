package pl.mateuszj.rekrutacja;

import java.util.List;
record RepositoryResponse(
        String repositoryName,
        String ownerLogin,
        List<BranchResponse> branches
) {}